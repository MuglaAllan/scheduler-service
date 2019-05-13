package je.dvs.echo;

import com.fasterxml.jackson.databind.ObjectMapper;
import je.dvs.echo.domain.*;
import je.dvs.echo.helpers.BookTestSlots;
import je.dvs.echo.repository.TestCalendarRepositoryImpl;
import je.dvs.echo.repository.TestDataRepository;
import je.dvs.echo.service.CalendarServiceImpl;
import je.dvs.echo.service.CalendarServices;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author carl
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CalendarServiceImpl.class, TestDataRepository.class, TestCalendarRepositoryImpl.class, DailySchedule.class, ObjectMapper.class})
@EnableConfigurationProperties
public class TestP30Renewals extends CalendarServices {

    @Autowired
    private CalendarServiceImpl calendarService;
    private final BookTestSlots bookTestSlots = new BookTestSlots();

    @Before
    public void setUp() {
        bookTestSlots.buildSlots(calendarService);
    }


    @Test
    public void testP30Bookings() throws Exception {



        UUID uuid = UUID.randomUUID();

        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 1);
        LocalDateTime appointmentDateTime = LocalDateTime.of(dateTime, LocalTime.of(8, 30));

        String appointment = appointmentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        calendarService.bookP30RenewalSlot(appointment, 30L, WorkshopType.PIT, uuid.toString());

        DailySchedule dailySchedule = calendarService.getByDate(dateTime);

        assertNotNull(dailySchedule);

        assertTrue(dailySchedule.getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(uuid) &&
                        slot instanceof BookedP30Slot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 60L &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        calendarService.removeAllBookedSlots();


    }

    @Test
    public void removeLocked() {
        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 1);
        calendarService.removeLockedSlotsFromDate(dateTime);
    }

    @Test
    public void testP30Offers(){

        calendarService.unlockAllSlots();

        AvailableSlots offers;
        List<String> dates = new ArrayList<>();
        String slots;
        UUID id = UUID.randomUUID();

        //First do a simple request
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 1), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerP30RenewalSlots(dates, id.toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(9, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 2), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(2).toLocalDate());


        //now fill a day with just a one hour block free
        calendarService.deleteAll();

        List<Slot> slotList = new ArrayList<>();
        final LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 2);
        for (LocalTime slot = LocalTime.of(8, 30); slot.isBefore(LocalTime.of(15, 30)); slot = slot.plusMinutes(30)) {
            if (!slot.toString().equals(LocalTime.of(15, 0).toString())) {
                slotList.add(new BookedVrsSlot(slot.format(DateTimeFormatter.ofPattern("HHmm")), 30l, WorkshopType.PIT, UUID.randomUUID()));
            }
        }
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slotList));

        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 0), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerP30RenewalSlots(dates, id.toString());
        offers = convertJsonToList(new JSONObject(slots));

        assertEquals(LocalTime.of(15, 00), offers.getSlots().get(0).toLocalTime());


    }

    @After
    public void TearDown()
    {
        calendarService.deleteAll();
    }






}
