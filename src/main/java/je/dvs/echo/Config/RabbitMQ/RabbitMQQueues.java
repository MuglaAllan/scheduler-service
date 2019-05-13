package je.dvs.echo.config.rabbitmq;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Properties;

@Component
public class RabbitMQQueues extends Properties {

    public final String ERROR_QUEUE = CreateRabbitMQQueue("errorQueue", "camel");
    public final String CALENDAR_QUEUE_IN_SLOT_OFFERS = CreateRabbitMQQueue("CalendarInQueueSlotOffers", "camel");
    public final String CALENDAR_QUEUE_IN_BOOK = CreateRabbitMQQueue("CalendarInQueueBook", "camel");
    public final String CALENDARDATES_QUEUE_IN = CreateRabbitMQQueue("CalendarDatesInQueue","camel");
    public final String CALENDAR_QUERY_IN_SLOTS = CreateRabbitMQQueue("CalendarSlotsInBook","camel");

    private static String CreateRabbitMQQueue(String QueueName, String RoutingKey){
        String hostv;
        String portv;
        String username;
        String password;

        hostv = System.getenv("V_RABBIT_HOST");
        portv = System.getenv("V_RABBIT_PORT");
        username = System.getenv("V_RABBIT_USERNAME");
        password = System.getenv("V_RABBIT_PASSWORD");

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/" )
                .scheme("rabbitmq")
                .host(hostv)
                .port(portv)
                .path("/" + QueueName)
                .queryParam("username",username)
                .queryParam("password", password)
                .queryParam("routingKey",RoutingKey)
                .queryParam("queue","Q" + QueueName);


        System.out.println(uriBuilder.toUriString());
        return uriBuilder.toUriString();

    }

}
