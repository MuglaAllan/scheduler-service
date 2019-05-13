package je.dvs.echo.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * A locked slot type
 *
 * @author carl
 */
public class LockedSlot extends DefaultSlot implements Slot, Serializable {
    private final Long lockedDateTime;

    public LockedSlot() {
        this.lockedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    public LockedSlot(final String time, final long duration, final WorkshopType workshopType, final UUID registrationId) {
        super(time, duration, workshopType, registrationId);
        this.lockedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    public Long getLockedDateTime() {
        return lockedDateTime;
    }
}
