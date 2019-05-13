package je.dvs.echo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import je.dvs.echo.service.CalendarServiceException;

/**
 * @author carl
 */
public class CalendarErrorProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(final Exchange exchange) {

        final Throwable cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);

        logger.debug("Error cause {}", cause);

        exchange.setProperty("errorMessage", cause.getMessage());
        exchange.setException(new CalendarServiceException(cause.toString()));

        exchange.getIn().setHeader("Error", cause);
        exchange.getOut().setHeader("Error", cause);

        exchange.getOut().setHeader(exchange.HTTP_RESPONSE_CODE, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        exchange.getIn().setHeader(exchange.HTTP_RESPONSE_CODE, HttpServletResponse.SC_METHOD_NOT_ALLOWED);

        exchange.getOut().setBody(cause.getMessage());
        exchange.getIn().setBody(cause.getMessage());

    }
}
