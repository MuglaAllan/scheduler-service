package je.dvs.echo.service;

import je.dvs.echo.domain.DailySchedule;
import je.dvs.echo.domain.WorkshopType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Calendar Service
 *
 * @author carl
 */

public interface CalendarService {

    String offerVrsSlots(final List<String> dates, final long duration, final WorkshopType workshopType, final String registrationID);

    void bookVrsSlot(final String date, final long duration, final WorkshopType workshopType, final String registrationID) throws Exception;

    void bookSlot(final String request, final String date, final long duration, final WorkshopType workshopType, final String registrationID) throws Exception;

    void lockAndBookSlotForDate(final String dateTime, long duration, WorkshopType workshopType, final String registrationId) throws Exception;

    String bookedSlots(final String date);

    Map<String, DailySchedule> listAll();

    DailySchedule getById(final Long id);

    DailySchedule saveOrUpdate(final DailySchedule dailySchedule);

    void delete(String id);

    void deleteSlots(String id, DailySchedule dailySchedule);

    void deleteAll();

    void deleteSlot(final String request, final String dateTime, final String registrationId, final String workshopType);

    DailySchedule getByEpoch(final long epoch);

    DailySchedule getByDate(final LocalDate date);

    DailySchedule getByDateAndRemoveLocked(final LocalDate date, final UUID registrationId);

    void removeLockedSlotsFromDate(final LocalDate date);

    void unlockAllSlots();

    void removeAllBookedSlots();

    void removeSlots(final Class slotType);

    void removeSlots(final Class slotType, final UUID registrationIs);

    String offerP30RenewalSlots(final List<String> dates, final String registrationID);

    void bookP30RenewalSlot(final String dateTime, final long duration, final WorkshopType workshopType, final String registrationId) throws Exception;
}
