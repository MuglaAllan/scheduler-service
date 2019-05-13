package je.dvs.echo.domain;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.util.List;
import je.dvs.echo.config.serializers.LocalDateTimeDeSerializer;
import je.dvs.echo.config.serializers.LocalDateTimeSerializer;

public class AvailableSlots {

    private final List<LocalDateTime> slots;

    public AvailableSlots(final List<LocalDateTime> slots) {
        this.slots = slots;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeSerializer.class)
    public List<LocalDateTime> getSlots() {
        return slots;
    }

}
