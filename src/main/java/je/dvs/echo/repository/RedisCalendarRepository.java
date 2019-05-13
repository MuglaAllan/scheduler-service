package je.dvs.echo.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import javax.annotation.PostConstruct;
import je.dvs.echo.domain.DailySchedule;

/**
 * Implementation of the Calendar Repository
 *
 * @author carl
 */
@Repository
public class RedisCalendarRepository implements CalendarRepository {

    private static final String KEY = "DailySchedule";

    private final RedisTemplate redisTemplate;
    private HashOperations<String, String, DailySchedule> hashOperations;

    @Autowired
    public RedisCalendarRepository(final RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init() {
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void save(final DailySchedule dailySchedule) {
        hashOperations.put(KEY, dailySchedule.getId().toString(), dailySchedule);
    }

    @Override
    public DailySchedule find(final Long id) {
        return hashOperations.get(KEY, id.toString());
    }

    @Override
    public Map<String, DailySchedule> findAll() {
        return hashOperations.entries(KEY);
    }

    @Override
    public void update(final DailySchedule dailySchedule) {
        hashOperations.put(KEY, dailySchedule.getId().toString(), dailySchedule);
    }

    @Override
    public void delete(final String id) {
        hashOperations.delete(KEY, id);
    }

    @Override public DailySchedule findByDate(final Long id) {
        return hashOperations.get(KEY, id.toString());
    }

    @Override public void deleteAll() {
    }
}
