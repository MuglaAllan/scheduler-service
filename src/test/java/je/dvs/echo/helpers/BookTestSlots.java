package je.dvs.echo.helpers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import je.dvs.echo.domain.*;
import je.dvs.echo.service.CalendarService;
import je.dvs.echo.service.CalendarServices;

/**
 * @author carl
 */
public class BookTestSlots extends CalendarServices {

    public void buildSlots(final CalendarService calendarService){

        calendarService.deleteAll();

        LocalDate dateTime = getNextBusinessDay(LocalDate.now(), 1);

        List<Slot> slots = new ArrayList<>();

        slots.add(new BookedP30Slot(LocalTime.of(14, 15).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new LockedSlot(LocalTime.of(10, 0).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new LockedSlot(LocalTime.of(15, 0).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.RAMP, UUID.randomUUID()));
        slots.add(new LockedSlot(LocalTime.of(10, 0).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.RAMP, UUID.randomUUID()));

        slots.add(new BookedVrsSlot(LocalTime.of(14, 15).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(10, 0).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(15, 0).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.RAMP, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(10, 0).format(DateTimeFormatter.ofPattern("HH:mm")), 15l, WorkshopType.RAMP, UUID.randomUUID()));
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slots));

        slots = new ArrayList<>();
        dateTime = getNextBusinessDay(LocalDate.now(), 2);
        slots.add(new BookedVrsSlot(LocalTime.of(8, 30).format(DateTimeFormatter.ofPattern("HHmm")), 60l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(10, 0).format(DateTimeFormatter.ofPattern("HHmm")), 15l, WorkshopType.RAMP, UUID.randomUUID()));
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slots));

        slots = new ArrayList<>();
        dateTime = getNextBusinessDay(LocalDate.now(), 3);
        slots.add(new BookedVrsSlot(LocalTime.of(8, 30).format(DateTimeFormatter.ofPattern("HHmm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(9, 0).format(DateTimeFormatter.ofPattern("HHmm")), 15l, WorkshopType.RAMP, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(10, 00).format(DateTimeFormatter.ofPattern("HHmm")), 240l, WorkshopType.RAMP, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(15, 00).format(DateTimeFormatter.ofPattern("HHmm")), 30l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(15, 30).format(DateTimeFormatter.ofPattern("HHmm")), 30l, WorkshopType.PIT, UUID.randomUUID()));
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slots));

        slots = new ArrayList<>();
        dateTime = getNextBusinessDay(LocalDate.now(), 4);
        slots.add(new BookedVrsSlot(LocalTime.of(8, 30).format(DateTimeFormatter.ofPattern("HHmm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(10, 00).format(DateTimeFormatter.ofPattern("HHmm")), 120l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(15, 00).format(DateTimeFormatter.ofPattern("HHmm")), 30l, WorkshopType.PIT, UUID.randomUUID()));
        slots.add(new BookedVrsSlot(LocalTime.of(15, 30).format(DateTimeFormatter.ofPattern("HHmm")), 30l, WorkshopType.PIT, UUID.randomUUID()));
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slots));


        slots = new ArrayList<>();
        dateTime = getNextBusinessDay(LocalDate.now(), 20);
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slots));


        //Create a day with only one 15 minute slot free
        slots = new ArrayList<>();
        dateTime = getNextBusinessDay(LocalDate.now(), 5);
        for (LocalTime slot = LocalTime.of(8, 30); slot.isBefore(LocalTime.of(16, 00)); slot = slot.plusMinutes(15)) {
            if (!slot.toString().equals(LocalTime.of(15, 30).toString())) {
                slots.add(new BookedVrsSlot(slot.format(DateTimeFormatter.ofPattern("HHmm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
            }
        }
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slots));


        //Create a day with only one 15 minute slot free at the end of the day
        slots = new ArrayList<>();
        dateTime = getNextBusinessDay(LocalDate.now(), 10);
        for (LocalTime slot = LocalTime.of(8, 30); slot.isBefore(LocalTime.of(16, 00)); slot = slot.plusMinutes(15)) {
            if (!slot.toString().equals(LocalTime.of(15, 45).toString())) {
                slots.add(new BookedVrsSlot(slot.format(DateTimeFormatter.ofPattern("HHmm")), 15l, WorkshopType.PIT, UUID.randomUUID()));
            }
        }
        calendarService.saveOrUpdate(new DailySchedule(buildKey(dateTime), slots));



    }


}
