package je.dvs.echo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import je.dvs.echo.domain.*;
import je.dvs.echo.repository.DataRepository;
import org.apache.camel.json.simple.JsonArray;
import org.apache.camel.json.simple.JsonObject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides helper methods for the Calendar Service
 */
public abstract class CalendarServices {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    String getAllSlotsForDate(final LocalDate date, final DataRepository dataRepository, final ObjectMapper objectMapper, final CalendarService calendarService){
        final TimeZone timeZone = TimeZone.getDefault();
        final LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.ofSecondOfDay(0));
        final Date zdate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        long epoch = dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();

        if (timeZone.inDaylightTime(zdate)) {
            epoch += timeZone.getDSTSavings() / 1000;
        }

        final DailySchedule dailySchedule = calendarService.getByEpoch(epoch);



        final List<DefaultSlot> slots = new ArrayList<>();

        if(dailySchedule != null)
        {
            dailySchedule.getSlots().forEach(slot -> {
                slots.add((DefaultSlot) slot);
            });
        }


        return convertListToJson(objectMapper, slots);
    }

    /**
     * What is needed to be able to schedule a slot in the calendar.
     *
     * Need to have a DATE to make the booking.
     * DONT NEED THIS ----> CALL : getSlots(DATE, DURATION, WORKSHOPTYPE, TIME, CALENDARSERVICE, UUID)
     * USE -> final AvailableSlots availableSlots = new AvailableSlots(offers); to create available slots structure.
     * Lock the slot -> lockSlots(availableSlots, duration, workshopType, UUID.fromString(registrationId), calendarService);
     *
     * Now we need to book the slot.
     *
     */
    String lockSlotForDate(final LocalDateTime slotDateTime, final long duration, final WorkshopType workshopType, final String registrationId, final DataRepository dataRepository, final ObjectMapper objectMapper, final CalendarService calendarService) {

        final List<LocalDateTime> offers = new ArrayList<>();

        offers.add(slotDateTime);

        final AvailableSlots availableSlots = new AvailableSlots(offers);

        lockSlots(availableSlots, duration, workshopType, UUID.fromString(registrationId), calendarService);

        return convertListToJson(objectMapper, availableSlots);
    }

    /**
     * Get's available slots in the calendar
     * The format is next business day first available slot, then alternate morning and afternoon slots offered
     * It uses environment variables to determine how many slots to offer, and a random day gap between them
     *
     * @param dates string array of dates, if multiple dates then will use the last one as basis for offers, whilst unlocking the others, if one date then that is used as basis for offers
     * @param duration the slot duration
     * @param workshopType workshop type
     * @param registrationId registration id from vrs
     * @param dataRepository repository for environment variables
     * @param objectMapper used to convert list to json
     * @param calendarService instance of the calendar service
     * @return json string
     */
    String getAvailableSlots(final List<String> dates, final long duration, final WorkshopType workshopType, final String registrationId, final DataRepository dataRepository, final ObjectMapper objectMapper, final CalendarService calendarService){

        final List<LocalDateTime> offers = new ArrayList<>();

        //If the list > 1 then we need to unlock the others and use the last one as the date for calculating the slots
        final LocalDate date = getSlotRequestedDate(dates, UUID.fromString(registrationId), calendarService);

        //Set a random amount of days away to stagger returned days when not an initial request
        final int daysAway = (dates.size() > 1) ? ThreadLocalRandom.current().nextInt(1, dataRepository.getNextOfferedSlotDayGap() + 1) : dataRepository.getNextOfferedSlotDayGap();

        for (int offer = 1; offer <= dataRepository.getOfferedSlotsTotal(); offer++) {
            if (offer == 1) {
                //Next available slot
                offers.add(getSlots(getNextBusinessDay(date, dataRepository.getFirstOfferedSlotDayGap()), duration, workshopType, dataRepository.getMorningStartTime(date), dataRepository.getAfternoonEndTime(date), calendarService, UUID.fromString(registrationId)));
            } else if (offer % 2 == 0) {
                //Next morning slot n days away
                offers.add(getSlots(getNextBusinessDay(offers.get(0).toLocalDate(), daysAway), duration, workshopType, dataRepository.getMorningStartTime(date), dataRepository.getMorningEndTime(date), calendarService, UUID.fromString(registrationId)));
            } else {
                //Next afternoon slot n days away
                offers.add(getSlots(getNextBusinessDay(offers.get(0).toLocalDate(), daysAway), duration, workshopType, calcAfternoonSlotStartTime(date, duration, dataRepository), dataRepository.getAfternoonEndTime(date), calendarService, UUID.fromString(registrationId)));
            }
        }

        final AvailableSlots availableSlots = new AvailableSlots(offers);

        lockSlots(availableSlots, duration, workshopType, UUID.fromString(registrationId), calendarService);

        return convertListToJson(objectMapper, availableSlots);
    }


    /**
     * Gets free slots for a given date, will keep looking for 365 days to find a free slot
     *
     * @param date date required
     * @param duration duration of required slot
     * @param workshopType workshop type
     * @param startTime time of day to start looking
     * @param endTime time of day to stop looking
     * @param calendarService instance of calendar service
     * @param registrationId registration id from vrs
     * @return date and time of available slot
     */
    private LocalDateTime getSlots(final LocalDate date, final long duration, final WorkshopType workshopType, final LocalTime startTime, final LocalTime endTime, final CalendarService calendarService, final UUID registrationId) {

        logger.debug("Received {} {} {} {} {}", date, duration, workshopType, startTime, endTime);

        LocalTime slotTime = startTime;
        LocalDate scheduledDay = date;

        //Search for slot over the next year
        for (int day = 0; day <= 365; day++) {

            final LocalDate nextBusinessDay = getNextBusinessDay(date, day);
            final DailySchedule nextBusinessDaySchedule = calendarService.getByDateAndRemoveLocked(nextBusinessDay, registrationId);
            scheduledDay = nextBusinessDay;

            if (nextBusinessDaySchedule != null && !nextBusinessDaySchedule.getSlots().isEmpty()) {

                List<Slot> slots = nextBusinessDaySchedule.getSlots();
                slots.sort(Comparator.comparing(Slot::getTime));
                slotTime = calcNextAvailableSlotTime(workshopType, startTime, endTime, slots, duration);

                if (slotTime != null) {
                    break;
                }

            } else {

                if(!slotTooLongForTheDay(startTime, endTime, nextBusinessDay, duration)){
                    slotTime = startTime;
                    break;
                }

            }

        }

        if (slotTime == null){
            throw new CalendarServiceException("No slot found for " + date + " for duration " + duration + " and workshop type " + workshopType);
        }

        return scheduledDay.atTime(slotTime);

    }

    private boolean slotTooLongForTheDay(final LocalTime startTime, final LocalTime endTime, final LocalDate date, final long duration){
        //Check duration isn't too long for given day
        final LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        return startDateTime.plusMinutes(duration).isAfter(LocalDateTime.of(date, endTime));
    }


    /**
     * Gets the next available slot time from a list of slots
     * Method is looped based, incrementing the day in 5 minute intervals to determine if there's space
     *
     * @param workshopType workshop type
     * @param startTime time of day to start
     * @param endTime time of day to end
     * @param slots current list of booked or locked slots for that date
     * @param duration duration of requested slot
     * @return slot time - null if none available
     */
    private LocalTime calcNextAvailableSlotTime(final WorkshopType workshopType, final LocalTime startTime, final LocalTime endTime, final List<Slot> slots, final long duration) {
        LocalTime slotTime = null;

        //There could be no booked slots of requested slot workshop type
        if (nextBookedSlot(slots, -1, workshopType).isEmpty()) {
            return startTime;
        }

        //Iterate the day in 5 minute slots
        for (LocalTime potentialSlot = startTime; potentialSlot.isBefore(endTime); potentialSlot = potentialSlot.plusMinutes(5)) {

            for (int i = 0; i <= (slots.size() - 1); i++) {

                if (slots.get(i).getWorkshopType().equals(workshopType)) {

                    final LocalTime currentBookedSlotStartTime = parseTime(slots.get(i).getTime());
                    final LocalTime currentBookedSlotEndTime = parseTime(slots.get(i).getTime()).plusMinutes(slots.get(i).getDuration());

                    //Exit loop if there is a booked slot at this potential slot time
                    if (potentialSlot.equals(currentBookedSlotStartTime)) {
                        break;
                    }
                    //Exit the loop if the potential time is during a booked slot
                    if (potentialSlot.plusMinutes(duration).isAfter(currentBookedSlotStartTime) &&
                            potentialSlot.isBefore(currentBookedSlotEndTime)) {
                        break;
                    }

                    //Exit the loop if the duration goes beyond end of day
                    if (potentialSlot.plusMinutes(duration).isAfter(endTime)) {
                        break;
                    }

                    //Check if time slot plus the requested slot's duration is before the next booked slot
                    if (potentialSlot.plusMinutes(duration).isBefore(currentBookedSlotStartTime)) {
                        slotTime = potentialSlot;
                        break;
                    }

                    //Get the next scheduled slot of the same type, if any
                    final List<Slot> nextBookedSlots = nextBookedSlot(slots, i, workshopType);

                    //If no more booked slots of same type then we can use the end time of the current one
                    if (nextBookedSlots.isEmpty()) {
                        slotTime = potentialSlot;
                        break;
                    }

                    //Check current potential slot does not overlap with next booked slot
                    for (int slot = 0; slot <= nextBookedSlots.size() - 1; slot++) {

                        //Check there's no overlap with potential slot and next booked slot
                        if (parseTime(nextBookedSlots.get(slot).getTime()).isBefore(potentialSlot.plusMinutes(duration))
                                || parseTime(nextBookedSlots.get(slot).getTime()).equals(potentialSlot.plusMinutes(duration))) {
                            break;
                        }

                        //If no more slots left we can potentially use the end of this one
                        if (slot == nextBookedSlots.size() - 1 && nextBookedSlots.size() > 1) {
                            //Is there a slot between the previous booked and current booked slots?
                            final Slot previousBookedSlot = nextBookedSlots.get(slot - 1);
                            if (parseTime(previousBookedSlot.getTime()).plusMinutes(previousBookedSlot.getDuration() + duration).isBefore(LocalTime.parse(nextBookedSlots.get(slot).getTime(), DateTimeFormatter.ofPattern("HHmm")))) {
                                slotTime = parseTime(previousBookedSlot.getTime()).plusMinutes(previousBookedSlot.getDuration());
                                break;
                            }
                        }

                        //Check if next booked plus the potential slot and the required duration is after
                        if (parseTime(nextBookedSlots.get(slot).getTime()).isAfter(potentialSlot.plusMinutes(duration))) {
                            slotTime = potentialSlot;
                            break;
                        }
                    }

                    if (slotTime != null) {
                        break;
                    }
                }
            }
            if (slotTime != null) {
                break;
            }
        }

        return slotTime;
    }


    /**
     * Peek ahead in the currently booked/locked slots array and find the next one with supplied workshop type
     *
     * @param slots currently booked/locked slots
     * @param currentSlotIndex the slot in question
     * @param workshopType workshop type
     * @return teh next booked slot
     */
    private List<Slot> nextBookedSlot(final List<Slot> slots, final int currentSlotIndex, final WorkshopType workshopType) {

        final List<Slot> nextBookedSlot = new ArrayList<>();

        for (int i = currentSlotIndex + 1; i <= slots.size() - 1; i++) {
            if (slots.get(i).getWorkshopType().equals(workshopType)) {
                nextBookedSlot.add(slots.get(i));
            }
        }

        return nextBookedSlot;

    }

    /**
     * Gets the required future non weekend day
     *
     * @param date day to start looking
     * @param workdays how many work days in the future
     * @return non weekend date
     */
    protected LocalDate getNextBusinessDay(final LocalDate date, final long workdays) {

        LocalDate result = date;
        if (date.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            result = date.plusDays(2);
        } else if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            result = date.plusDays(1);
        }
        int days = 0;
        while (days < workdays) {
            result = result.plusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                ++days;
            }
        }

        return result;
    }


    /**
     * Returns a date from list of dates
     * When multiple entries in dates, will return the last one and unlock the others
     * When single, just return the date
     *
     * @param dates multiple or single dates
     * @param registrationId registrion id from vrs used to unlock any previous
     * @param calendarService calendar service instance
     * @return non weekend date
     */
    private LocalDate getSlotRequestedDate(final List<String> dates, final UUID registrationId, final CalendarService calendarService) {

        final String date = dates.get(dates.size() - 1);
        LocalDate slotStartDate = getDate(date);

        if (dates.size() > 1) {
            unlockPreviouslyOffered(dates, registrationId, calendarService);
        }

        return (dates.size() > 1) ? getNextBusinessDay(slotStartDate, 1) : getNextBusinessDay(slotStartDate, 0);

    }

    /**
     * Get the date from a string
     *
     * @param date string date
     * @return non weekend date
     */
    public LocalDate getDate(final String date) {

        LocalDate slotDate;

        try {
            slotDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ex) {
            try {
                final LocalDateTime dateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                slotDate = LocalDate.from(dateTime);
            } catch (Exception e) {
                final LocalDateTime dateTime = LocalDateTime.parse(date);
                slotDate = LocalDate.from(dateTime);
            }
        }

        return getNextBusinessDay(slotDate, 0);

    }

    /**
     * Get the date and time from a string
     * @param dateTime date time string
     * @return date time
     */
    LocalDateTime getDateTime(final String dateTime) {

        LocalDateTime slotDateTime;

        try {
            slotDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ex) {
            slotDateTime = null;
        }

        return slotDateTime;

    }


    /**
     * Unlocks any previously offered slots
     *
     * @param lockedSlots array of locked slots
     * @param registrationId registration id of locked slots
     * @param calendarService calendar service instance
     */
    private void unlockPreviouslyOffered(final List<String> lockedSlots, final UUID registrationId, final CalendarService calendarService) {

        final List<LocalDateTime> dates = new ArrayList<>();
        lockedSlots.forEach(date -> dates.add(getDateTime(date)));

        dates.forEach(locked -> {

            if(locked != null){
                final DailySchedule dailySchedule = calendarService.getByDate(locked.toLocalDate());

                if (dailySchedule != null){
                    final List<LockedSlot> slotsToUnlock = new ArrayList<>();

                    if (!dailySchedule.getSlots().isEmpty()) {
                        dailySchedule.getSlots().forEach(slot -> {
                            if (slot instanceof LockedSlot &&
                                    slot.getTime().equals(locked.toLocalTime().format(DateTimeFormatter.ofPattern("HHmm"))) &&
                                    slot.getRegistrationId().equals(registrationId)) {
                                slotsToUnlock.add((LockedSlot) slot);
                            }
                        });

                        dailySchedule.getSlots().removeAll(slotsToUnlock);
                    }

                    calendarService.saveOrUpdate(dailySchedule);

                }
            }

        });

    }

    /**
     * Convert the calculated available slots to a json array
     *
     * @param objectMapper json object mapper
     * @param slotsForDay slots for the day
     * @return json string
     */
    private String convertListToJson(final ObjectMapper objectMapper, final List<DefaultSlot> slotsForDay) {

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writerWithDefaultPrettyPrinter();

        final JsonObject jsonObject = new JsonObject();
        final JsonArray jsonArray = new JsonArray();

        // Convert the array of slots to JSON.
        for (DefaultSlot defaultSlot : slotsForDay) {
//            if (defaultSlot instanceof BookedP30Slot) {
                final JsonObject childobject = new JsonObject();
                childobject.put("slot_type", defaultSlot.getClass().toString());
                childobject.put("registrationid", defaultSlot.getRegistrationId().toString());
                childobject.put("appointment_time", defaultSlot.getTime());
                childobject.put("appointment_duration", defaultSlot.getDuration());
                childobject.put("workshop_type", defaultSlot.getWorkshopType().toString());
                jsonArray.add(childobject);
//            }
        }

//        availableSlots.getSlots().forEach(slot -> jsonArray.add(slot.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        jsonObject.put("slots", jsonArray);

        logger.info("Returning slots: {}", jsonObject.toJson());

        return jsonObject.toJson();

    }

    /**
     * Convert the calculated available slots to a json array
     *
     * @param objectMapper json object mapper
     * @param availableSlots available slots
     * @return json string
     */
    private String convertListToJson(final ObjectMapper objectMapper, final AvailableSlots availableSlots) {

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writerWithDefaultPrettyPrinter();

        final JsonObject jsonObject = new JsonObject();
        final JsonArray jsonArray = new JsonArray();

        availableSlots.getSlots().forEach(slot -> jsonArray.add(slot.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        jsonObject.put("slots", jsonArray);

        logger.debug("Returning slots: {}", jsonObject.toJson());

        return jsonObject.toJson();

    }

    /**
     * Converts a json array of available slots to a java class
     *
     * @param jsonSlots json array object
     * @return available slots array
     */
    protected AvailableSlots convertJsonToList(final JSONObject jsonSlots) {

        final List<LocalDateTime> slots = new ArrayList<>();

        for (int i = 0; i < jsonSlots.getJSONArray("slots").length(); i++) {
            slots.add(LocalDateTime.parse(jsonSlots.getJSONArray("slots").get(i).toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }

        return new AvailableSlots(slots);

    }

    /**
     * Calculates afternoon start time
     *
     * @param date date in question
     * @param duration duration required
     * @param dataRepository environment vars
     * @return time
     */
    private LocalTime calcAfternoonSlotStartTime(final LocalDate date, final long duration, final DataRepository dataRepository) {
        final int durationDiff = Duration.ofMinutes(duration).compareTo(Duration.between(dataRepository.getAfternoonStartTime(date), dataRepository.getAfternoonEndTime(date)));
        return (durationDiff < 0) ? dataRepository.getAfternoonStartTime(date) : dataRepository.getAfternoonEndTime(date).minusMinutes(duration);
    }


    /**
     * Lock the offered slots so that the client can book one if required
     *
     * @param offers list of offers
     * @param duration  duration of slot
     * @param workshopType workshop type
     * @param registrationId registration id from vrs
     * @param calendarService instance of calendar service
     */
    private void lockSlots(final AvailableSlots offers, final long duration, final WorkshopType workshopType, final UUID registrationId, final CalendarService calendarService) {

        final List<DailySchedule> dailySchedules = new ArrayList<>();

        offers.getSlots().forEach(offer -> {
            final List<Slot> lockedSlots = new ArrayList<>();
            lockedSlots.add(new LockedSlot(offer.toLocalTime().format(DateTimeFormatter.ofPattern("HHmm")), duration, workshopType, registrationId));
            dailySchedules.add(new DailySchedule(buildKey(offer.toLocalDate()), lockedSlots));
        });

        dailySchedules.forEach(schedule -> {
            final DailySchedule dailySchedule = calendarService.getById(schedule.getId());
            if (dailySchedule != null) {
                dailySchedule.getSlots().addAll(schedule.getSlots());
                calendarService.saveOrUpdate(dailySchedule);
            } else {
                calendarService.saveOrUpdate(new DailySchedule(schedule.getId(), schedule.getSlots()));
            }
        });

    }

    /**
     * Get the date and time from the epoch
     * @param epoch the epoch
     * @return date and time
     */
    LocalDateTime getDateTimeFromEpoch(final Long epoch) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
    }


    /**
     * Book the slot
     *
     * @param slotType slot type class
     * @param dailySchedule current daily schedule to add the slot to - can be null
     * @param slotDateTime required slot date and time
     * @param duration duration of the slot to book
     * @param workshopType workshop type of teh slot to book
     * @param registrationId the registration id from vrs to book the slot for
     * @return a new daily schedule
     * @throws Exception can be caused by reflection
     */
    DailySchedule bookSlot(final Class slotType,  DailySchedule dailySchedule, final LocalDateTime slotDateTime, final long duration, final WorkshopType workshopType, final String registrationId) throws Exception {

        final String slotTime = slotDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HHmm"));
        Long DailyScheduleID = dailySchedule.getId();

        LocalDateTime RequestedStartTime = slotDateTime;
        LocalDateTime RequestedEndTime = slotDateTime.plusMinutes(duration);


        //Use reflection to get the Booking type's constructor
        final Class<?> clazz = Class.forName(slotType.getCanonicalName());
        final Constructor<?> ctor = clazz.getConstructor(String.class, long.class, WorkshopType.class, UUID.class);
        final Object slot = ctor.newInstance(slotTime, duration, workshopType, UUID.fromString(registrationId));

        if (dailySchedule != null) {

            //check there's no locked slot with time and type
            if (dailySchedule.getSlots()
                    .stream().anyMatch(s -> s instanceof LockedSlot &&
                            s.getTime().equals(slotTime) &&
                            s.getWorkshopType().equals(workshopType))) {

                throw new CalendarServiceException("Slot locked for " + slotDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            //check there's no booking with time and type
            if (dailySchedule.getSlots()
                    .stream().anyMatch(s ->
                            s.getTime().equals(slotTime) &&
                            s.getWorkshopType().equals(workshopType))) {

                throw new CalendarServiceException("Slot already booked for " + slotDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            //add slot matching for clashes
            if (dailySchedule.getSlots()
                    .stream().anyMatch(s ->
                                    ((RequestedStartTime).isAfter(s.getTimes(getDateTimeFromEpoch(DailyScheduleID))) &&(RequestedStartTime).isBefore(s.getTimes(getDateTimeFromEpoch(DailyScheduleID)).plusMinutes(s.getDuration()))) ||
                                    ((RequestedEndTime).isAfter(s.getTimes(getDateTimeFromEpoch(DailyScheduleID))) &&(RequestedEndTime).isBefore(s.getTimes(getDateTimeFromEpoch(DailyScheduleID)).plusMinutes(s.getDuration())))
                                     && s.getWorkshopType().equals(workshopType))) {

                throw new CalendarServiceException("Appointment unavailable as clashes with slot:" + slotDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            dailySchedule.getSlots().add((Slot) slot);

        } else {
            final List<Slot> slots = new ArrayList<>();
            slots.add((Slot) slot);

            dailySchedule = new DailySchedule(buildKey(slotDateTime.toLocalDate()), slots);
        }

        return dailySchedule;

    }

    /**
     * Format the time to HHmm
     *
     * @param time unformatted time
     * @return formatted time
     */
    private LocalTime parseTime(final String time) {
        return LocalTime.parse(time.replace(":", ""), DateTimeFormatter.ofPattern("HHmm"));
    }

    /**
     * Returns the epoch second of the required date, at time 00:00
     * Used by redis as the key for a daily schedule
     *
     * @param date date in question
     * @return epoch time
     */
    protected long buildKey(final LocalDate date) {
//        long bl = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
//        System.out.println("Date : " + String.valueOf(bl));
        return date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }



}
