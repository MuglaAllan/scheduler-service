package je.dvs.echo;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
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
import java.util.Map;
import java.util.UUID;
import je.dvs.echo.domain.*;
import je.dvs.echo.helpers.BookTestSlots;
import je.dvs.echo.repository.TestCalendarRepositoryImpl;
import je.dvs.echo.repository.TestDataRepository;
import je.dvs.echo.service.CalendarServiceException;
import je.dvs.echo.service.CalendarServiceImpl;
import je.dvs.echo.service.CalendarServices;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CalendarServiceImpl.class, TestDataRepository.class, TestCalendarRepositoryImpl.class, DailySchedule.class, ObjectMapper.class})
@EnableConfigurationProperties
public class TestVrsOffers extends CalendarServices {

    @Autowired
    private CalendarServiceImpl calendarService;
    private BookTestSlots bookTestSlots = new BookTestSlots();

    @Before
    public void setUp() {
        bookTestSlots.buildSlots(calendarService);
    }

    @Test
    public void testDaysOfWeek() {
        LocalDate date = getNextBusinessDay(LocalDate.of(2018, 5, 21), 6);
        assertEquals(LocalDate.of(2018, 5, 29), date);
    }

    @Test
    public void testDaysOfWeektoday() {
        LocalDate date = getNextBusinessDay(LocalDate.now(), 6);
        assertEquals(LocalDate.of(2018, 5, 29), date);
    }


    @Test
    public void testAutoUnlock() throws InterruptedException {

        calendarService.unlockAllSlots();

        AvailableSlots offers;
        List<String> dates = new ArrayList<>();
        String slots;
        UUID id = UUID.randomUUID();

        //First do a simple request

        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 1), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, id.toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(9, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 2), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(15, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 5), offers.getSlots().get(2).toLocalDate());

        DailySchedule byDate = calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2));

        //Check that we have locked the above offers
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0930") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 6)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1530") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        //Now check the are locked for a different reg id
        String newId = UUID.randomUUID().toString();
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, newId);
        offers = convertJsonToList(new JSONObject(slots));

        //check that still locked for previous id
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0930") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 6)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1530") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        //Now check new id has locked slots
        byDate = calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(UUID.fromString(newId)) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0945") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 6)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(UUID.fromString(newId)) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0845") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 6)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(UUID.fromString(newId)) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1330") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        //Now request again with the same id and times and we should see the same
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, newId);
        offers = convertJsonToList(new JSONObject(slots));
        byDate = calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2));

        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(UUID.fromString(newId)) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0945") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 6)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(UUID.fromString(newId)) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0845") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 6)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(UUID.fromString(newId)) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1330") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));



        //Now request again with previous offers in the list
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 2), LocalTime.of(9, 30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 6), LocalTime.of(8, 30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 5), LocalTime.of(15, 30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, id.toString());
        offers = convertJsonToList(new JSONObject(slots));

        //Check that they've been unlocked

        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0930") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 6)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 5)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1530") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        //Now check new offers locked
        DailySchedule dailySchedule = calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 10));
        assertTrue(calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 7)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        //Now wait and they should auto unlock
        Thread.sleep(61000);

        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.RAMP, id.toString());

        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 7)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("0830") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));
        assertTrue(!calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 10)).getSlots()
                .stream().anyMatch(slot -> slot.getRegistrationId().equals(id) &&
                        slot instanceof LockedSlot &&
                        slot.getTime().equals("1545") &&
                        slot.getDuration() == 15l &&
                        slot.getWorkshopType().equals(WorkshopType.PIT)));

        calendarService.unlockAllSlots();


    }



    @Test
    public void testOffers() throws InterruptedException {

        calendarService.unlockAllSlots();

        AvailableSlots offers;
        List<String> dates = new ArrayList<>();

        //Extra long appointment
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 0), LocalTime.of(0, 0)).toString());
        String slots = calendarService.offerVrsSlots(dates, 180L, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(10, 15), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 1), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 00), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(2).toLocalDate());

        //Checked now locked
        DailySchedule dailySchedule = calendarService.getByDate(getNextBusinessDay(LocalDate.now(), 2));
        dailySchedule.getSlots().forEach(slot -> {
            if (slot.getTime().equals(LocalTime.of(9, 30)) && slot.getDuration() == 180L && slot.getWorkshopType().equals(WorkshopType.PIT)) {
                if (!(slot instanceof LockedSlot)) {
                    Assert.fail();
                }
            }
        });
        slots = calendarService.offerVrsSlots(dates, 180L, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertNotEquals(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 1), LocalTime.of(10, 15)), offers.getSlots().get(0).toLocalTime());
        assertNotEquals(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 6), LocalTime.of(8, 30)), offers.getSlots().get(0).toLocalTime());
        assertNotEquals(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 6), LocalTime.of(13, 00)), offers.getSlots().get(0).toLocalTime());


        //Test the lock duration
        Thread.sleep(61000);

        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 1), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(9, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 2), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(15, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 5), offers.getSlots().get(2).toLocalDate());


        //Now just unlock all of them
        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 0), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 1), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 45), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 4), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 4), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 1), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 100l, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(9, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 2), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 1), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 100l, WorkshopType.RAMP, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(10, 15), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 2), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 5), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 5), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 3), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(8, 45), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 4), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 7), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 7), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 3), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 100l, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(12, 00), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 4), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 7), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 7), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 3), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 100l, WorkshopType.RAMP, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 4), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 7), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 7), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 4), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15, WorkshopType.RAMP, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 5), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 8), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 8), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 4), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(15, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 5), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 8), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 8), offers.getSlots().get(2).toLocalDate());

        //Full day, get slot from next day
        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 4), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 30, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 6), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 9), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 9), offers.getSlots().get(2).toLocalDate());

        //Only one slot free at the end of the day
        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 9), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 15, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(15, 45), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 10), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 13), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 13), offers.getSlots().get(2).toLocalDate());

        //Make sure we don't overlap end of day
        calendarService.unlockAllSlots();
        dates.clear();
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 9), LocalTime.of(0, 0)).toString());
        slots = calendarService.offerVrsSlots(dates, 30, WorkshopType.PIT, UUID.randomUUID().toString());
        offers = convertJsonToList(new JSONObject(slots));
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 11), offers.getSlots().get(0).toLocalDate());
        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(1).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 14), offers.getSlots().get(1).toLocalDate());
        assertEquals(LocalTime.of(13, 30), offers.getSlots().get(2).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 14), offers.getSlots().get(2).toLocalDate());

        calendarService.unlockAllSlots();

    }

    @Test
    public void testOffersWithEmptySchedule() {

        List<String> dates = new ArrayList<>();
        dates.add(LocalDate.now().toString());

        calendarService.listAll().forEach((key, value) -> calendarService.delete(key));

        String slots = calendarService.offerVrsSlots(dates, 15l, WorkshopType.PIT, UUID.randomUUID().toString());
        AvailableSlots offers = convertJsonToList(new JSONObject(slots));

        assertEquals(LocalTime.of(8, 30), offers.getSlots().get(0).toLocalTime());
        assertEquals(getNextBusinessDay(LocalDate.now(), 1), offers.getSlots().get(0).toLocalDate());

        calendarService.unlockAllSlots();

    }


    @Test
    public void testBuildKey() {
        Long key = buildKey(LocalDate.of(2018, 5, 17));
        assertThat(1526511600L, is(key));
    }


    @Test
    public void testFindByDate() {
        LocalDate localDateTime = getNextBusinessDay(LocalDate.now(), 1);
        DailySchedule dailySchedule = calendarService.getByDate(localDateTime);
        assertNotNull(dailySchedule);

        localDateTime = getNextBusinessDay(LocalDate.now(), 2);
        dailySchedule = calendarService.getByDate(localDateTime);
        assertNotNull(dailySchedule);

        localDateTime = getNextBusinessDay(LocalDate.now(), 3);
        dailySchedule = calendarService.getByDate(localDateTime);
        assertNotNull(dailySchedule);

        localDateTime = getNextBusinessDay(LocalDate.now(), 4);
        dailySchedule = calendarService.getByDate(localDateTime);
        assertNotNull(dailySchedule);

    }


    @Test
    public void testCrud() {

        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 20);
        List<Slot> slots = new ArrayList<>();

        slots.add(new BookedVrsSlot(LocalTime.of(10, 0).toString(), 15l, WorkshopType.PIT, UUID.randomUUID()));
        DailySchedule dailySchedule = new DailySchedule(buildKey(dateTime), slots);
        calendarService.saveOrUpdate(dailySchedule);
        DailySchedule savedDailySchedule = calendarService.getById(dailySchedule.getId());

        assertNotNull(savedDailySchedule);
        assertTrue(savedDailySchedule.getSlots().get(0).getDuration() == 15l);

        savedDailySchedule.getSlots().get(0).setDuration(300l);

        calendarService.saveOrUpdate(savedDailySchedule);
        dailySchedule = calendarService.getById(savedDailySchedule.getId());

        assertNotNull(dailySchedule);
        assertTrue(dailySchedule.getSlots().get(0).getDuration() == 300l);


        calendarService.delete(dailySchedule.getId().toString());
        savedDailySchedule = calendarService.getById(dailySchedule.getId());

        assertNull(savedDailySchedule);

        calendarService.unlockAllSlots();

    }

    @Test
    public void testList() {
        final Map<String, DailySchedule> dailyScheduleMap = calendarService.listAll();
        assertNotNull(dailyScheduleMap);
    }

    @Test
    public void testWeekendDate() {
        List<String> dates = new ArrayList<>();

        dates.add(LocalDateTime.of(2018, 6, 9, 13, 00).toString());
        String slots = calendarService.offerVrsSlots(dates, 15L, WorkshopType.PIT, UUID.randomUUID().toString());

        assertNotNull(slots);

        final AvailableSlots offers = convertJsonToList(new JSONObject(slots));

        assertNotNull(offers);

        assertEquals(LocalDate.of(2018, 6, 12), LocalDate.from(offers.getSlots().get(0)));

        calendarService.unlockAllSlots();

    }

    @Test(expected = CalendarServiceException.class)
    public void testNoSlotsFound(){

        List<String> dates = new ArrayList<>();

        //Extra long appointment
        dates.add(LocalDateTime.of(getNextBusinessDay(LocalDate.now(), 0), LocalTime.of(0, 0)).toString());
        String slots = calendarService.offerVrsSlots(dates, 6000L, WorkshopType.PIT, UUID.randomUUID().toString());

    }

    @Test
    public void bookedSlots()
    {
        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 21);
        String result = calendarService.bookedSlots(dateTime.toString());

        System.out.println(result);
    }




    @After
    public void tearDown() {
        calendarService.deleteAll();
    }
}
