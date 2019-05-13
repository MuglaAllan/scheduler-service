package je.dvs.echo.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment slot interface
 *
 * Implemented by all appointment slot types
 *
 * @author carl
 */
public interface Slot {

    long getDuration();

    void setDuration(final long duration);

    String getTime();

    LocalDateTime getTimes(LocalDateTime dailySchedule);

    void setTime(final String time);

    WorkshopType getWorkshopType();

    UUID getRegistrationId();


}


