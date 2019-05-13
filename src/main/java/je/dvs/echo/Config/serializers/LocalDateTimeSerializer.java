package je.dvs.echo.config.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author carl
 */
public class LocalDateTimeSerializer extends JsonSerializer<List<LocalDateTime>> {

    private static final long serialVersionUID = 1L;

    public LocalDateTimeSerializer() {
    }

    @Override
    public void serialize(List<LocalDateTime> value, JsonGenerator gen, SerializerProvider sp) throws IOException {

        gen.writeStartArray();

        value.forEach(val -> {
            try {
                gen.writeString(val.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        gen.writeEndArray();
    }

}
