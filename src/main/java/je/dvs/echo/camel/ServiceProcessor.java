package je.dvs.echo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ServiceProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

    /*    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,Exception.class);
        exchange.getIn().setHeader("Failed",cause.getMessage());
        String body = exchange.getIn().getBody(String.class);
        int count = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
        exchange.getIn().setBody(body + count);
        // the maximum redelivery was set to 5
        int max = exchange.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER, Integer.class);*/

        String payload = exchange.getIn().getBody(String.class);
        System.out.println(payload);

    }
}