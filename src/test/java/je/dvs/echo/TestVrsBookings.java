package je.dvs.echo;

import static org.junit.Assert.*;

import je.dvs.echo.helpers.BookTestSlots;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import je.dvs.echo.domain.*;
import je.dvs.echo.repository.TestCalendarRepositoryImpl;
import je.dvs.echo.repository.TestDataRepository;
import je.dvs.echo.service.CalendarServiceException;
import je.dvs.echo.service.CalendarServiceImpl;
import je.dvs.echo.service.CalendarServices;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CalendarServiceImpl.class, TestDataRepository.class, TestCalendarRepositoryImpl.class, DailySchedule.class, ObjectMapper.class})
@EnableConfigurationProperties
public class TestVrsBookings extends CalendarServices {

    @Autowired
    private CalendarServiceImpl calendarService;
    private final BookTestSlots bookTestSlots = new BookTestSlots();


    @Before
    public void setUp() {
        bookTestSlots.buildSlots(calendarService);
      //  calendarService.listAll().forEach((key, value) -> calendarService.delete(key));
    }


    @Test(expected = CalendarServiceException.class)
    public void bookLockedSlot() throws Exception {

        List<String> dates = new ArrayList<>();
        String slots;
        UUID id = UUID.randomUUID();

        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 1);
        LocalDateTime appointmentDateTime = LocalDateTime.of(getNextBusinessDay(dateTime, 1), LocalTime.of(8, 30));


        dates.add(LocalDateTime.of(dateTime, LocalTime.of(0, 0)).toString());
        slots = calendarService.offerP30RenewalSlots(dates, id.toString());

        assertNotNull(slots);

        String appointment = appointmentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        calendarService.bookVrsSlot(appointment, 30L, WorkshopType.PIT,  UUID.randomUUID().toString());

    }


    @Test
    public void bookAppointment() throws Exception {

        UUID uuid = UUID.randomUUID();

        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 1);
        LocalDateTime appointmentDateTime = LocalDateTime.of(dateTime, LocalTime.of(8, 30));

        String appointment = appointmentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        calendarService.bookVrsSlot(appointment, 30L, WorkshopType.PIT, uuid.toString());

        DailySchedule dailySchedule = calendarService.getByDate(dateTime);

        assertNotNull(dailySchedule);

        assertTrue(dailySchedule.getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(uuid) &&
                        slot instanceof BookedVrsSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 30L &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        calendarService.removeAllBookedSlots();

    }

    @Test(expected = CalendarServiceException.class)
    public void preventMultipleBookings() throws Exception {

        UUID uuid = UUID.randomUUID();

        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 1);
        LocalDateTime appointmentDateTime = LocalDateTime.of(dateTime, LocalTime.of(8, 30));

        String appointment = appointmentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        calendarService.bookVrsSlot(appointment, 30L, WorkshopType.PIT, uuid.toString());
        calendarService.bookVrsSlot(appointment, 100L, WorkshopType.PIT, uuid.toString());

        calendarService.unlockAllSlots();
        calendarService.removeAllBookedSlots();

    }

    @Test
    public void testBookingAndRemoveLockedSlots() throws Exception {

        calendarService.unlockAllSlots();
        calendarService.removeAllBookedSlots();

        AvailableSlots offers;
        List<String> dates = new ArrayList<>();
        String slots;
        UUID id = UUID.randomUUID();

        //First do a simple request to lock slots
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 1), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, id.toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(0).toLocalTime());

        //confirm slot locked
        DailySchedule byDate = calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        byDate = calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1330") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        //Now book the 1530 offered slot
        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 5);
        LocalDateTime appointmentDateTime = LocalDateTime.of(dateTime, LocalTime.of(15, 30));
        String appointment = appointmentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        calendarService.bookVrsSlot(appointment, 15l, WorkshopType.PIT, id.toString());

        //Now confirm locked slots removed
        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1330") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        calendarService.unlockAllSlots();
        calendarService.removeAllBookedSlots();

    }



    public void tearDown() {
        calendarService.deleteAll();
    }

}