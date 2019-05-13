package je.dvs.echo.camel;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import je.dvs.echo.config.rabbitmq.RabbitMQQueues;
import je.dvs.echo.service.CalendarServiceException;
import je.dvs.echo.service.CalendarServiceImpl;

import static org.apache.camel.builder.PredicateBuilder.or;

@Component
@Import(RabbitMQQueues.class)
public class CamelRoutes extends RouteBuilder {

    @Value("#{rabbitMQQueues.CALENDAR_QUERY_IN_SLOTS}")
    public String CALENDAR_QUERY_IN_SLOTS;

    @Value("#{rabbitMQQueues.CALENDAR_QUEUE_IN_SLOT_OFFERS}")
    public String CALENDAR_QUEUE_IN_SLOT_OFFERS;

    @Value("#{rabbitMQQueues.CALENDAR_QUEUE_IN_BOOK}")
    public String CALENDAR_QUEUE_IN_BOOK;

    @Value("#{rabbitMQQueues.CALENDARDATES_QUEUE_IN}")
    public String CALENDARDATES_QUEUE_IN;

    @Value("#{rabbitMQQueues.ERROR_QUEUE}")
    public String ERROR_QUEUE;

    final private String AUDIT_ENTRY = "direct:auditEntry";

    public String LOGGER_QUEUE = "log:?level=INFO&showBody=true&showHeaders=true&showExchangeId=true&multiline=true";

    // @formatter:off
    @Override
    public void configure() {

        onException(CalendarServiceException.class)
                .log("CALENDAR SERVICE BOOKING EXCEPTION")
                .handled(true)
                .process(exchange -> {
                    Exception cause = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    exchange.setProperty("errorMessage",cause.getMessage());
                })
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpServletResponse.SC_CONFLICT))
                .setHeader(Exchange.HTTP_RESPONSE_TEXT,simple("${property.errorMessage}"))
                .end();

        onException(Exception.class)
                .log("CALENDAR SERVICE EXCEPTION")
                .handled(true)
                .process(exchange -> {
                    Exception cause = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    exchange.setProperty("errorMessage",cause.getMessage());
                })
                .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("Server Error"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                .setHeader(Exchange.HTTP_RESPONSE_TEXT,simple("${property.errorMessage}"))
                .end();

        onException(Exception.class)
                .log("Camel exception")
                .handled(true)
                .process(exchange -> {
                    CamelExecutionException cause = (CamelExecutionException) exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    exchange.setProperty("errorMessage",cause.getMessage());
                })
                .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("Server Error"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                .setHeader(Exchange.HTTP_RESPONSE_TEXT,simple("${property.errorMessage}"))
                .end();

        /**
         * Routes...
         */

        from(CALENDAR_QUERY_IN_SLOTS)
                .to(LOGGER_QUEUE)
                .convertBodyTo(String.class)
                .log("Appointments by date ${body}")
                .process(exchange -> new CamelProcessor().processBookings(exchange))
                .setProperty("dates", simple("${header.dates}"))
                .bean(CalendarServiceImpl.class, "bookedSlots(${property.dates})")
                .to(LOGGER_QUEUE)
                .routeId("returnBookedSlots")
        .end();

        from(CALENDAR_QUEUE_IN_SLOT_OFFERS)
                .to(LOGGER_QUEUE)
                .convertBodyTo(String.class)

                .choice()
                    .when(header("REQUEST").isEqualTo(constant("offerVrsSlots")))
                        .log("Vrs Appointment Request Received ${body}")
                        .process(exchange -> new CamelProcessor().processVrsOffers(exchange))
                        .setProperty("duration", simple("${header.duration}"))
                        .setProperty("type", simple("${header.type}"))
                        .setProperty("registrationId", simple("${header.registrationId}"))
                        .setProperty("dates", simple("${header.dates}"))
                        .bean(CalendarServiceImpl.class, "offerVrsSlots(${property.dates}, ${property.duration}, ${property.type}, ${property.registrationId})")
                        .to(LOGGER_QUEUE)
                        .routeId("returnVrsOffers")
                    .when(header("REQUEST").isEqualTo(constant("offerP30Slots")))
                        .log("P30 Appointment Request Received ${body}")
                        .process(exchange -> new CamelProcessor().processP30Offers(exchange))
                        .setProperty("registrationId", simple("${header.registrationId}"))
                        .setProperty("dates", simple("${header.dates}"))
                        .bean(CalendarServiceImpl.class, "offerP30RenewalSlots(${property.dates}, ${property.registrationId})")
                        .to(LOGGER_QUEUE)
                        .routeId("returnP30Offers")
//                    .end()
        .end();


        final Predicate bookSlotCheck = or(header("REQUEST").isEqualTo(constant("bookVrsSlot")),
                                           header("REQUEST").isEqualTo(constant("bookP30RenewalSlot")));

        from(CALENDAR_QUEUE_IN_BOOK).outputType("application/json")
                .to(LOGGER_QUEUE)
                .convertBodyTo(String.class)
//                .to(ExchangePattern.OutOnly, AUDIT_ENTRY)
                .choice()
                    .when(bookSlotCheck)
                        .process(exchange -> new CamelProcessor().processAnyBookings(exchange))

                        .setProperty("date", simple("${header.date}"))
                        .setProperty("duration", simple("${header.duration}"))
                        .setProperty("type", simple("${header.type}"))
                        .setProperty("registrationId", simple("${header.registrationId}"))

                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpServletResponse.SC_OK))
                        .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("ok"))

                        .log("Appointment Booking Request Received ${body}")
                        .bean(CalendarServiceImpl.class, "bookSlot(${header.REQUEST}, ${property.date}, ${property.duration}, ${property.type}, ${property.registrationId})")

                        .to(LOGGER_QUEUE)
                    .when(header("REQUEST").isEqualTo(constant("bookVrsSlotForDate")))
                        .process(exchange -> new CamelProcessor().processAnyBookings(exchange))

                        .setProperty("date", simple("${header.date}"))
                        .setProperty("duration", simple("${header.duration}"))
                        .setProperty("type", simple("${header.type}"))
                        .setProperty("registrationId", simple("${header.registrationId}"))

                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpServletResponse.SC_OK))
                        .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("ok"))

                        .log("Appointment Booking Request Received ${body}")
                        .bean(CalendarServiceImpl.class, "lockAndBookSlotForDate(${property.date}, ${property.duration}, ${property.type}, ${property.registrationId})")
//                    .end()
        .end();


        from(CALENDARDATES_QUEUE_IN).outputType("application/json")
           .to(LOGGER_QUEUE)
           .convertBodyTo(String.class)
           .to(ExchangePattern.OutOnly, AUDIT_ENTRY)
           .choice()
                .when(header("REQUEST").isEqualTo(constant("removeAppointment")))
                    .process(exchange -> new CamelProcessor().processAppointmentRemove(exchange))
                    .to(LOGGER_QUEUE)
                    .setProperty("oldDate", simple("${header.oldDate}"))
                    .setProperty("registrationId", simple("${header.registrationId}"))
                    .setProperty("workshopType", simple("${header.workshopType}"))
                    .setProperty("appointmentType", simple("${header.appointmentType}"))
                    .to(LOGGER_QUEUE)

                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpServletResponse.SC_OK))
                    .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("ok"))
                    .log("calendar call next")
                    .bean(CalendarServiceImpl.class, "deleteSlot(${header.REQUEST},${property.oldDate},${property.registrationId},${property.workshopType})")

                    .to(LOGGER_QUEUE)
                .routeId("removeAppointment")

                .when(header("REQUEST").isEqualTo(constant("updateAppointment")))
                    .log("Update Appointment")
                    .process(exchange -> new CamelProcessor().processsAppointmentChange(exchange))
                    .to(LOGGER_QUEUE)
                    .setProperty("oldDate", simple("${header.oldDate}"))
                    .setProperty("registrationId", simple("${header.registrationId}"))
                    .setProperty("newDate", simple("${header.newDate}"))
                    .setProperty("workshopType", simple("${header.workshopType}"))
                    .setProperty("appointmentType", simple("${header.appointmentType}"))
                    .to(LOGGER_QUEUE)

                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpServletResponse.SC_OK))
                    .setHeader(Exchange.HTTP_RESPONSE_TEXT, constant("ok"))

                    .bean(CalendarServiceImpl.class, "updateAppointmentSlot(${property.oldDate},${property.registrationId},${property.newDate},${property.workshopType},${property.appointmentType})")

                    .to(LOGGER_QUEUE)
                .routeId("updateAppointment")
       .end();

        from(AUDIT_ENTRY)
                .log("Audit Q")
                .setProperty("group_name", constant("calendar"))
                .setProperty("user_id", constant("4349d_needtopassthisvalue"))
//                .process(exchange -> new AuditProcessor().process(exchange))
        .end();

    }
    // @formatter:on
}
