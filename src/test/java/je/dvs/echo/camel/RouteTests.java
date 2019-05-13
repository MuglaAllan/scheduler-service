package je.dvs.echo.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.apache.camel.test.spring.UseAdviceWith;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;
import je.dvs.echo.config.rabbitmq.RabbitMQQueues;
import je.dvs.echo.domain.DailySchedule;
import je.dvs.echo.repository.TestCalendarRepositoryImpl;
import je.dvs.echo.repository.TestDataRepository;
import je.dvs.echo.service.CalendarServiceImpl;

@Ignore
@RunWith(CamelSpringRunner.class)
@MockEndpoints
@UseAdviceWith
@SpringBootTest(classes = {CalendarServiceImpl.class, TestDataRepository.class, TestCalendarRepositoryImpl.class, DailySchedule.class, ObjectMapper.class, CamelAutoConfiguration.class, RabbitMQQueues.class, CamelRoutes.class})
public class RouteTests {

    String START_ENDPOINT = "direct:start";
    String RESULT_ENDPOINT = "mock:result";

    @Autowired
    CamelContext context;

    @Autowired
    ProducerTemplate template;

    @Value("#{rabbitMQQueues.CALENDARDATES_QUEUE_IN}")
    public String CALENDARDATES_QUEUE_IN;

    @Value("#{rabbitMQQueues.CALENDAR_QUEUE_IN_BOOK}")
    public String CALENDAR_QUEUE_IN_BOOK;


    @Before
    public void setUp() throws Exception {
        context.getRouteDefinition("bookOffers").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                replaceFromWith(START_ENDPOINT);
                interceptSendToEndpoint("bean:CalendarServiceImpl?method=bookVrsSlot")
                        .skipSendToOriginalEndpoint()
                        .to(RESULT_ENDPOINT);
            }
        });
        context.start();
    }

    @Test
    public void CALENDAR_QUEUE_IN_BOOKRoute_ReturnApplicationData() throws Exception {
        MockEndpoint resultEndpoint = context.getEndpoint(RESULT_ENDPOINT, MockEndpoint.class);

//        resultEndpoint.expectedMessageCount(1);

        JSONArray dates = new JSONArray();

        JSONObject request = new JSONObject();
        request.put("registrationId", UUID.randomUUID());
        request.put("duration", 30l);
        request.put("type", "PIT");
        request.put("date", "2018-06-15 08:30");

        template.setDefaultEndpointUri(START_ENDPOINT);

        template.sendBody(START_ENDPOINT, request);

        resultEndpoint.assertIsSatisfied();
    }


    @After
    public void tearDown() throws Exception {
        context.stop();
    }
}