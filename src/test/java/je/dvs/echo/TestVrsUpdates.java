package je.dvs.echo;

import com.fasterxml.jackson.databind.ObjectMapper;
import je.dvs.echo.domain.*;
import je.dvs.echo.helpers.BookTestSlots;
import je.dvs.echo.repository.TestCalendarRepositoryImpl;
import je.dvs.echo.repository.TestDataRepository;
import je.dvs.echo.service.CalendarService;
import je.dvs.echo.service.CalendarServiceImpl;
import je.dvs.echo.service.CalendarServices;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CalendarServiceImpl.class, TestDataRepository.class, TestCalendarRepositoryImpl.class, DailySchedule.class, ObjectMapper.class, CalendarService.class})
@EnableConfigurationProperties
public class TestVrsUpdates extends CalendarServices {

    @Autowired
    private CalendarServiceImpl calendarService;
    private BookTestSlots bookTestSlots = new BookTestSlots();
    LocalDate dateTime;
    LocalDate newDateTime;
    String workshopType = WorkshopType.RAMP.toString();
    String appointmentType = BookedP30Slot.class.getCanonicalName();
    String time = "14 15";

    @Before
    public void SetUp()
    {
        dateTime = getNextBusinessDay(LocalDate.now(), 1);
        newDateTime = getNextBusinessDay(LocalDate.now(),2);
        bookTestSlots.buildSlots(calendarService);

    }
    @Test
    public void UpdateAppointment() throws Exception {

        DailySchedule oldDailySchedule = calendarService.getByDate(dateTime);
        Slot slot = oldDailySchedule.getSlots().get(0);

        LocalDateTime oldAppointmentDateTime = LocalDateTime.of(dateTime, LocalTime.of(14,15));
        String oldAppointmentDate = oldAppointmentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        LocalDateTime newAppointmentDateTime = LocalDateTime.of(newDateTime, LocalTime.of(14,30));
        String newAppointmentDate = newAppointmentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        System.out.println("Registration ID: " + slot.getRegistrationId() + ","
                + "Old Date: " + Instant.ofEpochSecond(oldDailySchedule.getId()) + ","
                + "Time: " + slot.getTime() + ","
                + "AppointmentType: " + slot.getClass().getCanonicalName() + ","
                + "WorkshopType: " + slot.getWorkshopType());

        LocalTime newTime = newAppointmentDateTime.toLocalTime();

        calendarService.updateAppointmentSlot(oldAppointmentDate,slot.getRegistrationId().toString(),newAppointmentDate,workshopType, appointmentType);

        DailySchedule newDailySchedule = calendarService.getByDate(newDateTime);

        LocalDateTime newDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(newDailySchedule.getId()), ZoneId.systemDefault());

        Slot movedSlot =  newDailySchedule.getSlots()
                        .stream()
                        .filter(slot1 -> slot1.getRegistrationId().equals(slot.getRegistrationId()))
                        .findAny()
                        .orElse(null);

        System.out.println("Registration ID: " + movedSlot.getRegistrationId() + ","
                + "New Date: " + Instant.ofEpochSecond(newDailySchedule.getId()) + ","
                + "Time: " + movedSlot.getTime() + ","
                + "AppointmentType: " + movedSlot.getClass().getCanonicalName() + ","
                + "WorkshopType: " + movedSlot.getWorkshopType());

        Assert.assertTrue("Registration ID is not the same", movedSlot.getRegistrationId().equals( slot.getRegistrationId()));
        Assert.assertTrue("Slots are on the same date",!Instant.ofEpochSecond(oldDailySchedule.getId()).equals(Instant.ofEpochSecond(newDailySchedule.getId())));
        Assert.assertTrue("Times are the same", !movedSlot.getTime().equals(slot.getTime()));
        Assert.assertTrue("Appointment Type is the same",!movedSlot.getClass().getCanonicalName().equals(slot.getClass().getCanonicalName()));
        Assert.assertTrue("Workshop Type is the same", !movedSlot.getWorkshopType().equals(slot.getWorkshopType()));
    }


    @Test
    public void DeleteAppointment() throws Exception
    {
        DailySchedule dailySchedule = calendarService.getByDate(dateTime);
        Slot slot = dailySchedule.getSlots().get(0);
        int dailyScheduleSize = dailySchedule.getSlots().size();

        calendarService.deleteSlots(slot.getRegistrationId().toString(),dailySchedule);

        DailySchedule updateDailySchedule = calendarService.getByDate(dateTime);

        Assert.assertTrue(updateDailySchedule.getSlots().size() <  dailyScheduleSize);


    }

    @After
    public void Teardown()
    {
        calendarService.deleteAll();
    }


}
