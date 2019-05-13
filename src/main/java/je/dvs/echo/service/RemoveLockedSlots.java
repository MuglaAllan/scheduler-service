package je.dvs.echo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import je.dvs.echo.domain.LockedSlot;

public class RemoveLockedSlots implements Runnable {

    private final CalendarService calendarService;
    private final UUID registrationId;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RemoveLockedSlots(final UUID registrationId, final CalendarService calendarService) {
        this.registrationId = registrationId;
        this.calendarService = calendarService;
    }

    @Override
    public void run() {

        logger.info("Starting remove slots thread....");

        calendarService.removeSlots(LockedSlot.class, registrationId);

        logger.info("Ending remove slots thread....");


    }
}
