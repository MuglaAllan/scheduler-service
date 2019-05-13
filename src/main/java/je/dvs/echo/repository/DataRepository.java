package je.dvs.echo.repository;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data repository to retrieve service specific data
 *
 * @author carl
 */
public interface DataRepository {

    LocalTime getMorningStartTime(final LocalDate date);

    LocalTime getMorningEndTime(final LocalDate date);

    LocalTime getAfternoonStartTime(final LocalDate date);

    LocalTime getAfternoonEndTime(final LocalDate date);

    int getOfferedSlotsTotal();

    int getFirstOfferedSlotDayGap();

    int getNextOfferedSlotDayGap();

    long getLockedDuration();

}
