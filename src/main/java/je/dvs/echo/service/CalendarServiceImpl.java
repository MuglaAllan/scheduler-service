package je.dvs.echo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import je.dvs.echo.domain.*;
import je.dvs.echo.repository.CalendarRepository;
import je.dvs.echo.repository.DataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the Calendar Service
 *
 * @author carl
 */
@Service
public class CalendarServiceImpl extends CalendarServices implements CalendarService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final CalendarRepository calendarRepository;
    public final DataRepository dataRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public CalendarServiceImpl(final CalendarRepository calendarRepository, final DataRepository dataRepository, final ObjectMapper objectMapper) {
        this.calendarRepository = calendarRepository;
        this.dataRepository = dataRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String bookedSlots(final String date) {
        final LocalDate slotDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String result =  getAllSlotsForDate(slotDate, dataRepository, objectMapper, this);

        return result;
    }

    @Override
    public String offerVrsSlots(final List<String> dates, final long duration, final WorkshopType workshopType, final String registrationId) {

        logger.debug("Received {} {} {} {}", dates, duration, workshopType, registrationId);

        return getAvailableSlots(dates, duration, workshopType, registrationId, dataRepository, objectMapper, this);

    }

    @Override
    public void deleteSlot(final String request, final String dateTime, final String registrationId, final String workshopType) {
        logger.debug("Deleting slot {} {} {} {}", dateTime, workshopType, registrationId);

        //Get the schedule for that date
        final LocalDateTime slotDateTime = getDateTime(dateTime);
        DailySchedule dailySchedule = getByDate(slotDateTime.toLocalDate());

        Slot indexOfSlot = dailySchedule.getSlots()
                .stream()
                .filter(slot -> UUID.fromString(registrationId).equals(slot.getRegistrationId()))
                .findAny()
                .orElse(null);

        deleteSlots(indexOfSlot.getRegistrationId().toString(), dailySchedule);

        logger.debug("Appointment has been Deleted: {}", registrationId);
    }

    @Override
    public void bookSlot(final String request, final String dateTime, final long duration, final WorkshopType workshopType, final String registrationId) throws Exception {
        logger.debug("Booking slot {} {} {} {}", dateTime, duration, workshopType, registrationId);

        //Get the schedule for that date
        final LocalDateTime slotDateTime = getDateTime(dateTime);
        DailySchedule dailySchedule = getByDateAndRemoveLocked(slotDateTime.toLocalDate(), UUID.fromString(registrationId));

        if (request.equalsIgnoreCase("bookVrsSlot")) {
            saveOrUpdate(bookSlot(BookedVrsSlot.class, dailySchedule, slotDateTime, duration, workshopType, registrationId));
        } else {
            saveOrUpdate(bookSlot(BookedP30Slot.class, dailySchedule, slotDateTime, 60L, WorkshopType.PIT, registrationId));
        }

        new Thread(() -> new RemoveLockedSlots(UUID.fromString(registrationId), this).run()).run();
    }

    @Override
    public void lockAndBookSlotForDate(final String dateTime, long duration, WorkshopType workshopType, final String registrationId) throws Exception {
        logger.debug("lockAndBookSlotForDate VRS slot {} {} {} {}", dateTime, duration, workshopType, registrationId);

        //Get the schedule for that date
        final LocalDateTime slotDateTime = getDateTime(dateTime);

        // Lock the date.
        lockSlotForDate(slotDateTime, duration, workshopType, registrationId, dataRepository, objectMapper, this);

        this.bookVrsSlot(dateTime, duration, workshopType, registrationId);

    }

    @Override
    public void bookVrsSlot(final String dateTime, final long duration, final WorkshopType workshopType, final String registrationId) throws Exception {

        logger.debug("Booking VRS slot {} {} {} {}", dateTime, duration, workshopType, registrationId);

        //Get the schedule for that date
        final LocalDateTime slotDateTime = getDateTime(dateTime);
        DailySchedule dailySchedule = getByDateAndRemoveLocked(slotDateTime.toLocalDate(), UUID.fromString(registrationId));

        saveOrUpdate(bookSlot(BookedVrsSlot.class, dailySchedule, slotDateTime, duration, workshopType, registrationId));

        new Thread(() -> new RemoveLockedSlots(UUID.fromString(registrationId), this).run()).run();
    }

    @Override
    public DailySchedule getByDateAndRemoveLocked(final LocalDate date, final UUID registrationId) {

        final DailySchedule dailySchedule = getByDate(date);
        final List<LockedSlot> slots = new ArrayList<>();
        if (dailySchedule != null) {
            dailySchedule.getSlots().forEach(slot -> {

                if (slot instanceof LockedSlot) {
                    final LocalDateTime lockedTime = getDateTimeFromEpoch(((LockedSlot) slot).getLockedDateTime());

                    long dur = dataRepository.getLockedDuration();
                    if (lockedTime.plusMinutes(dur).isBefore(LocalDateTime.now())) {
                        slots.add((LockedSlot) slot);
                    }

                    if (slot.getRegistrationId().equals(registrationId)) {
                        slots.add((LockedSlot) slot);
                    }

                }

            });

            if (!slots.isEmpty()) {
                dailySchedule.getSlots().removeAll(slots);
                saveOrUpdate(dailySchedule);
            }

        }
        else
        {
            saveOrUpdate(new DailySchedule(buildKey(date), new ArrayList<>()));
        }

        return dailySchedule;

    }

    @Override
    public void removeLockedSlotsFromDate(final LocalDate date) {
        final DailySchedule dailySchedule = getByDate(date);
        final List<LockedSlot> slots = new ArrayList<>();
        if (dailySchedule != null) {
            dailySchedule.getSlots().forEach(slot -> {
                if (slot instanceof LockedSlot) {
                    final LocalDateTime lockedTime = getDateTimeFromEpoch(((LockedSlot) slot).getLockedDateTime());
                    slots.add((LockedSlot) slot);
                }
            });

            if (!slots.isEmpty()) {
                dailySchedule.getSlots().removeAll(slots);
                saveOrUpdate(dailySchedule);
            }
        }
    }

    @Override
    public void unlockAllSlots() {
        removeSlots(LockedSlot.class);
    }

    @Override public void removeAllBookedSlots() {
        removeSlots(BookedVrsSlot.class);
    }

    @Override
    public void removeSlots(final Class slotType) {
        //Remove next years worth of type
        for (int day = 0; day <= 365; day++) {
            final DailySchedule dailySchedule = getByDate(getNextBusinessDay(LocalDate.now(), day));
            final List<Slot> slots = new ArrayList<>();
            if (dailySchedule != null) {
                dailySchedule.getSlots().forEach(slot -> {
                    if (slotType.equals(LockedSlot.class) && slot instanceof LockedSlot) {
                        slots.add(slot);
                    } else if (slotType.equals(BookedVrsSlot.class) && slot instanceof BookedVrsSlot) {
                        slots.add(slot);
                    }
                });

                if (!slots.isEmpty()) {
                    dailySchedule.getSlots().removeAll(slots);
                    saveOrUpdate(dailySchedule);
                }

            }
        }
    }

    @Override
    public void removeSlots(final Class slotType, final UUID registrationId) {
        //Remove next years worth of type
        for (int day = 0; day <= 365; day++) {
            final DailySchedule dailySchedule = getByDate(getNextBusinessDay(LocalDate.now(), day));
            final List<Slot> slots = new ArrayList<>();
            if (dailySchedule != null) {
                dailySchedule.getSlots().forEach(slot -> {
                    if (slotType.equals(LockedSlot.class) && slot instanceof LockedSlot && slot.getRegistrationId().equals(registrationId)) {
                        slots.add(slot);
                    } else if (slotType.equals(BookedVrsSlot.class) && slot instanceof BookedVrsSlot && slot.getRegistrationId().equals(registrationId)) {
                        slots.add(slot);
                    }
                });

                if (!slots.isEmpty()) {
                    dailySchedule.getSlots().removeAll(slots);
                    saveOrUpdate(dailySchedule);
                }

            }
        }
    }

    @Override public String offerP30RenewalSlots(final List<String> dates, final String registrationId) {
        logger.debug("Received {}", dates);

        return getAvailableSlots(dates, 60L, WorkshopType.PIT, registrationId, dataRepository, objectMapper, this);
    }

    @Override public void bookP30RenewalSlot(final String dateTime, final long duration, final WorkshopType workshopType, final String registrationId) throws Exception{

        logger.debug("Booking P30 renewal slot {} {} v2", dateTime, registrationId);

        //Get the schedule for that date
        final LocalDateTime slotDateTime = getDateTime(dateTime);
        DailySchedule dailySchedule = getByDateAndRemoveLocked(slotDateTime.toLocalDate(), UUID.fromString(registrationId));

        saveOrUpdate(bookSlot(BookedP30Slot.class, dailySchedule, slotDateTime, duration, workshopType, registrationId));

        new Thread(() -> new RemoveLockedSlots(UUID.fromString(registrationId), this).run()).run();
    }

    public void updateAppointmentSlot(String oldDate, String registrationId, String newDate, String workshopType,String appointmentType) throws Exception
    {
        logger.debug("Update Appointment slot with {},{},{},{},{},{}",oldDate,registrationId,newDate,workshopType,appointmentType);

        DailySchedule oldDailySchedule;
        final LocalDateTime slotDateTime = getDateTime(oldDate);

        oldDailySchedule = getByDateAndRemoveLocked(slotDateTime.toLocalDate(), UUID.fromString(registrationId));

        Slot indexOfSlot = oldDailySchedule.getSlots()
                .stream()
                .filter(slot -> UUID.fromString(registrationId).equals(slot.getRegistrationId()))
                .findAny()
                .orElse(null);


        String originalAppointmentTime = indexOfSlot.getTime();
        WorkshopType originalWorkshopType = indexOfSlot.getWorkshopType();
        String originalAppointmentType = indexOfSlot.getClass().toString();

        Assert.notNull(originalAppointmentTime, "Appointment time can not be null");
        Assert.notNull(originalWorkshopType, "Workshop Type can not be null");
        Assert.notNull(originalAppointmentType, "Appointment Type can not be null");

        final LocalDateTime newslotDateTime = getDateTime(newDate);
        DailySchedule originalDailySchedule = getByDateAndRemoveLocked(newslotDateTime.toLocalDate(),UUID.fromString(registrationId));

        LocalTime TimeFromNewDate =  newslotDateTime.toLocalTime();
        String AppoinmentTime = TimeFromNewDate.equals(originalAppointmentTime) ? originalAppointmentTime : TimeFromNewDate.toString();
        LocalTime UpdateAppointmentTime = LocalTime.parse(AppoinmentTime,DateTimeFormatter.ofPattern("HH:mm"));


        DailySchedule newDailySchedule = oldDailySchedule.getId().equals(originalDailySchedule.getId()) ? oldDailySchedule : originalDailySchedule;
        WorkshopType newWorkshopType = workshopType.equals(originalWorkshopType) ? originalWorkshopType : WorkshopType.valueOf(workshopType);
        String originalAppointment = appointmentType.equals(indexOfSlot.getClass()) ? originalAppointmentType.getClass().getCanonicalName() : appointmentType ;


        final Class<?> clazz = Class.forName(originalAppointment);
        final Constructor<?> ctor = clazz.getConstructor(String.class, long.class, WorkshopType.class, UUID.class);
        final Object slot = ctor.newInstance(AppoinmentTime, indexOfSlot.getDuration(), newWorkshopType, UUID.fromString(registrationId));

        deleteSlots(indexOfSlot.getRegistrationId().toString(),oldDailySchedule);


        saveOrUpdate(bookSlot(slot.getClass(),newDailySchedule, newslotDateTime.toLocalDate().atTime(UpdateAppointmentTime), indexOfSlot.getDuration(),newWorkshopType,registrationId));


        logger.debug("Appointment has been updated: {}", registrationId);
    }

    @Override
    public Map<String, DailySchedule> listAll() {
        return calendarRepository.findAll();
    }

    @Override
    public DailySchedule getById(final Long id) {
        return calendarRepository.find(id);
    }

    @Override
    public DailySchedule saveOrUpdate(final DailySchedule dailySchedule) {
        calendarRepository.update(dailySchedule);
        return dailySchedule;
    }

    @Override
    public void delete(final String id) {
        calendarRepository.delete(id);
    }

    @Override
    public void deleteSlots(String id, DailySchedule dailySchedule) {
        List<Slot> defaultSlots = dailySchedule.getSlots();
        defaultSlots.removeIf(defaultSlot -> defaultSlot.getRegistrationId() != null && defaultSlot.getRegistrationId().toString().equals(id));

        DailySchedule newDailySchedule = new DailySchedule(dailySchedule.getId(),defaultSlots);
        calendarRepository.update(newDailySchedule);
    }

    @Override
    public DailySchedule getByEpoch(final long epoch) {
        return calendarRepository.findByDate(epoch);
    }

    @Override
    public DailySchedule getByDate(final LocalDate date) {
        return calendarRepository.findByDate(buildKey(date));
    }

    @Override public void deleteAll() {
        calendarRepository.deleteAll();
    }


}
