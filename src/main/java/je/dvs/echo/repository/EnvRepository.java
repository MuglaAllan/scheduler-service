package je.dvs.echo.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of the Data Repository Using environment properties on the server
 *
 * @author carl
 */
@Repository
public class EnvRepository implements DataRepository {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Override
    public LocalTime getMorningStartTime(final LocalDate date) {
        final String prop = System.getenv(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".start").toString().trim());
        return LocalTime.parse(prop, DateTimeFormatter.ofPattern("Hmm"));
    }

    @Override
    public LocalTime getMorningEndTime(final LocalDate date) {
        final String prop = System.getenv(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".lunch").toString().trim());
        return LocalTime.parse(prop, DateTimeFormatter.ofPattern("Hmm"));
    }

    @Override
    public LocalTime getAfternoonStartTime(final LocalDate date) {
        final String prop = System.getenv(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".lunch.duration").toString().trim());
        return getMorningEndTime(date).plusMinutes(Long.parseLong(prop));
    }

    @Override
    public LocalTime getAfternoonEndTime(final LocalDate date) {
        final String prop = System.getenv(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".end").toString().trim());
        return LocalTime.parse(prop, DateTimeFormatter.ofPattern("Hmm"));
    }

    @Override
    public int getOfferedSlotsTotal() {
        return Integer.parseInt(System.getenv("calendar.offeredAppointments.total"));
    }

    @Override
    public int getFirstOfferedSlotDayGap() {
        return Integer.parseInt(System.getenv("calendar.offeredAppointments.dayGap.first"));
    }

    @Override
    public int getNextOfferedSlotDayGap() {
        return Integer.parseInt(System.getenv("calendar.offeredAppointments.dayGap.next"));
    }

    @Override public long getLockedDuration() {
        return Long.parseLong(System.getenv("calendar.appointments.locked.duration"));
    }

}
