package je.dvs.echo.repository;

import je.dvs.echo.domain.DailySchedule;
import je.dvs.echo.domain.Slot;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author carl
 */

public class TestCalendarRepositoryImpl implements CalendarRepository {

    private Collection<DailySchedule> dailySchedules;
    private Collection<Slot> defaultSlots;

    public TestCalendarRepositoryImpl(final List<DailySchedule> dailySchedules) {
        this.dailySchedules = dailySchedules;
    }

    @Override
    public void save(final DailySchedule dailySchedule) {
        dailySchedules.add(dailySchedule);
    }

    @Override
    public DailySchedule find(final Long id) {
        return dailySchedules.stream().filter(schedule -> schedule.getId() != null && schedule.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public Map<String, DailySchedule> findAll() {

        Map<String, DailySchedule> dailyScheduleMap = new HashMap<>();
        dailySchedules.forEach(schedule -> dailyScheduleMap.put(String.valueOf(ThreadLocalRandom.current().nextLong()), schedule));

        return dailyScheduleMap;
    }

    @Override
    public void update(final DailySchedule dailySchedule) {
//        dailySchedules.remove(dailySchedule);
        dailySchedules.add(dailySchedule);
    }

    @Override
    public void delete(final String id) {
        dailySchedules.removeIf(schedule -> schedule.getId() != null && schedule.getId().toString().equals(id));
    }



    @Override public DailySchedule findByDate(final Long id) {

        return dailySchedules.stream().filter(schedule -> schedule.getId() != null && schedule.getId().equals(id))
                .findFirst().orElse(null);

    }

    @Override public void deleteAll() {
        dailySchedules.clear();
    }
}
