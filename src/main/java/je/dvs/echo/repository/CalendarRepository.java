package je.dvs.echo.repository;

import java.util.Map;
import je.dvs.echo.domain.DailySchedule;

/**
 * The calendar data repository
 */
public interface CalendarRepository {

    void save(DailySchedule dailySchedule);

    DailySchedule find(Long id);

    Map<String, DailySchedule> findAll();

    void update(DailySchedule dailySchedule);

    void delete(String id);

    DailySchedule findByDate(Long id);

    void deleteAll();


}
