package je.dvs.echo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class CamelProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public void process(final Exchange exchange) {
    }

    public void processBookings(final Exchange exchange) {
        final String result = exchange.getIn().getBody(String.class);

        logger.info("Camel Bookings Processor has received: {}", result);

        final JSONObject request = new JSONObject(result);

        final JSONArray dates = request.getJSONArray("dates");

        final List<String> dateList = new ArrayList<>();

        if (dates != null) {
            for (int i = 0; i < dates.length(); i++) {
                dateList.add(dates.get(i).toString());
            }
        }

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setHeader("dates", dateList);

        exchange.getOut().setBody(result);

        logger.info("Camel Bookings Processor is sending: {}", result);
    }

    public void processVrsOffers(final Exchange exchange) {
        final String result = exchange.getIn().getBody(String.class);

        logger.info("Camel Vrs Offer Processor has received: {}", result);

        final JSONObject request = new JSONObject(result);

        final long duration = request.getLong("duration");
        final String type = request.getString("type").toUpperCase();
        final String registrationId = request.getString("registrationId");
        final JSONArray dates = request.getJSONArray("dates");

        final List<String> dateList = new ArrayList<>();

        if (dates != null) {
            for (int i = 0; i < dates.length(); i++) {
                dateList.add(dates.get(i).toString());
            }
        }

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());

        exchange.getOut().setHeader("duration", duration);
        exchange.getOut().setHeader("type", type);
        exchange.getOut().setHeader("registrationId", registrationId);
        exchange.getOut().setHeader("dates", dateList);

        exchange.getOut().setBody(result);

        logger.info("Camel Vrs Offer Processor is sending: {}", result);
    }

    public void processP30Offers(final Exchange exchange) {
        final String result = exchange.getIn().getBody(String.class);

        logger.info("Camel P30 Offer Processor has received: {}", result);

        final JSONObject request = new JSONObject(result);

        final String registrationId = request.getString("registrationId");
        final JSONArray dates = request.getJSONArray("dates");

        final List<String> dateList = new ArrayList<>();

        if (dates != null) {
            for (int i = 0; i < dates.length(); i++) {
                dateList.add(dates.get(i).toString());
            }
        }

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());

        exchange.getOut().setHeader("registrationId", registrationId);
        exchange.getOut().setHeader("dates", dateList);

        exchange.getOut().setBody(result);

        logger.info("Camel P30 Offer Processor is sending: {}", result);
    }

    public void processAnyBookings(final Exchange exchange) {
        final String result = exchange.getIn().getBody(String.class);

        logger.info("Camel Bookings Processor has received: {}", result);

        final JSONObject request = new JSONObject(result);

        final long duration = request.getLong("duration");
        final String type = request.getString("type").toUpperCase();
        final String registrationId = request.getString("registrationId");
        final String date = request.getString("date");

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());

        exchange.getOut().setHeader("duration", duration);
        exchange.getOut().setHeader("type", type);
        exchange.getOut().setHeader("registrationId", registrationId);
        exchange.getOut().setHeader("date", date);

        exchange.getOut().setBody(result);

        logger.info("Camel Bookings Processor is sending: {}", result);

    }

    public void processsAppointmentChange(final Exchange exchange)
    {
        final String result = exchange.getIn().getBody(String.class);

        logger.info("Appointment Change Processor has received: {}", result);

        final JSONObject request = new JSONObject(result);

        final String oldDate = request.getString("oldDate");
        final String registrationId = request.getString("registrationId");
        final String newDate = request.getString("newDate").isEmpty() ? null : request.getString("newDate");
        final String workshopType = request.getString("workshopType").isEmpty() ? null :request.getString("workshopType");
        final String appointmentType = request.getString("appointmentType").isEmpty() ? null : request.getString("appointmentType");

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());

        exchange.getOut().setHeader("oldDate", oldDate);
        exchange.getOut().setHeader("registrationId", registrationId);
        if(!newDate.isEmpty()) {exchange.getOut().setHeader("newDate", newDate);}
        if(!workshopType.isEmpty()) {exchange.getOut().setHeader("workshopType", workshopType);}
        if(!appointmentType.isEmpty()) {exchange.getOut().setHeader("appointmentType", appointmentType);}

        exchange.getOut().setBody(result);

        logger.info("Update Appointment Change Processor is sending: {}", result);
    }

    public void processAppointmentRemove(final Exchange exchange) {
        final String result = exchange.getIn().getBody(String.class);

        logger.info("Appointment Remove Processor has received: {}", result);

        final JSONObject request = new JSONObject(result);

        final String oldDate = request.getString("oldDate");
        final String registrationId = request.getString("registrationId");
        final String workshopType = (request.has("type") == false) ? null :request.getString("type");
        final String appointmentType = (request.has("appointmentType") == false) ? "class je.dvs.echo.domain.BookedVrsSlot" : request.getString("appointmentType");

        exchange.getOut().setHeaders(exchange.getIn().getHeaders());

        exchange.getOut().setHeader("oldDate", oldDate);
        exchange.getOut().setHeader("registrationId", registrationId);
        if(!workshopType.isEmpty()) {exchange.getOut().setHeader("workshopType", workshopType);}
        if(!appointmentType.isEmpty()) {exchange.getOut().setHeader("appointmentType", appointmentType);}

        exchange.getOut().setBody(result);

        logger.info("Update Appointment Change Processor is sending: {}", result);
    }

}
