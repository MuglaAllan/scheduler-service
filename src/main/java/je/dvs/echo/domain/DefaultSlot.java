package je.dvs.echo.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Default appointment slot
 *
 * @author carl
 */
public class DefaultSlot implements Serializable, Slot {

    private String time;
    private long duration;
    private WorkshopType workshopType;
    private UUID registrationId;

    DefaultSlot() {}

    DefaultSlot(final String time, final long duration, final WorkshopType workshopType, final UUID registrationId) {
        this.time = time;
        this.duration = duration;
        this.workshopType = workshopType;
        this.registrationId = registrationId;
    }

    public DefaultSlot(UUID registrationId) {
        this.registrationId = registrationId;
    }

    public String getTime() {
        return time;
    }

    @Override
    public LocalDateTime getTimes(LocalDateTime dailySchedule) {

        LocalTime Time = LocalTime.parse(time,DateTimeFormatter.ofPattern("HHmm"));
//        System.out.println("DateTime:" + dailySchedule.toLocalDate().atTime(Time));
        return  dailySchedule.toLocalDate().atTime(Time);
    }


    public void setTime(final String time) {
        this.time = time;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    public void setDuration(final long duration) {
        this.duration = duration;
    }

    public WorkshopType getWorkshopType() {
        return workshopType;
    }

    @Override
    public UUID getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(final UUID registrationId) {
        this.registrationId = registrationId;
    }
}
