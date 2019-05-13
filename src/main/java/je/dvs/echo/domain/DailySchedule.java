package je.dvs.echo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.List;

/**
 * The hash that is saved to redis
 *
 * @author carl
 */
@RedisHash(value = "DailySchedule")
public class DailySchedule implements Serializable {

    /** Date converted to epoch date */
    @Id
    private Long id;

    /** List of slots currently in the given date */
    private List<Slot> slots;

    public DailySchedule() {
    }

    public DailySchedule(final Long id, final List<Slot> slots) {
        this.id = id;
        this.slots = slots;
    }

    public Long getId() {
        return id;
    }

    public List<Slot> getSlots() {

           return slots;
    }







}
