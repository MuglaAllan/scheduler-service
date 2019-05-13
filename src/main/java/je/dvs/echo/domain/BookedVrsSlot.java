package je.dvs.echo.domain;

import java.io.Serializable;
import java.util.UUID;

/**
 * A booked appointment slot
 *
 * @author carl
 */
public class BookedVrsSlot extends DefaultSlot implements Slot, Serializable {

    public BookedVrsSlot() {
    }

    public BookedVrsSlot(final String time, final long duration, final WorkshopType workshopType, final UUID registrationId) {
        super(time, duration, workshopType, registrationId);
    }
}
