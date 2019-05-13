package je.dvs.echo.domain;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author carl
 *
 * Oversize vehicle P30 Booked slot
 *
 */
public class BookedP30Slot extends DefaultSlot implements Slot, Serializable {

    public BookedP30Slot() {
    }

    public BookedP30Slot(final String time, final long duration, final WorkshopType workshopType, final UUID registrationId) {
        super(time, duration, workshopType, registrationId);
    }

}
