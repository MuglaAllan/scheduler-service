package je.dvs.echo.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author carl
 */
public class TestDataRepository implements DataRepository {

    @Autowired
    private Environment env;

    @Override
    public LocalTime getMorningStartTime(final LocalDate date) {
        final String prop = env.getProperty(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".start").toString().trim());
        return LocalTime.parse(prop, DateTimeFormatter.ofPattern("Hmm"));
    }

    @Override
    public LocalTime getMorningEndTime(final LocalDate date) {
        final String prop = env.getProperty(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".lunch").toString().trim());
        return LocalTime.parse(prop, DateTimeFormatter.ofPattern("Hmm"));
    }

    @Override
    public LocalTime getAfternoonStartTime(final LocalDate date) {
        final String prop = env.getProperty(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".lunch.duration").toString().trim());
        return getMorningEndTime(date).plusMinutes(Long.parseLong(prop));
    }

    @Override
    public LocalTime getAfternoonEndTime(final LocalDate date) {
        final String prop = env.getProperty(new StringBuilder("calendar.appointments.").append(date.getDayOfWeek().toString().toLowerCase()).append(".end").toString().trim());
        return LocalTime.parse(prop, DateTimeFormatter.ofPattern("Hmm"));
    }

    @Override
    public int getOfferedSlotsTotal() {
        return Integer.parseInt(env.getProperty("calendar.offeredAppointments.total"));
    }

    @Override
    public int getFirstOfferedSlotDayGap() {
        return Integer.parseInt(env.getProperty("calendar.offeredAppointments.dayGap.first"));
    }

    @Override
    public int getNextOfferedSlotDayGap() {
        return Integer.parseInt(env.getProperty("calendar.offeredAppointments.dayGap.next"));
    }

    @Override public long getLockedDuration() {
        return Long.parseLong(env.getProperty("calendar.appointments.locked.duration"));
    }

}
