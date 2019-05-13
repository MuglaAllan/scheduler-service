package je.dvs.echo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.HashMap;
import java.util.Map;


public class AuditProcessor implements Processor {

    @Override
    public void process(final Exchange exchange) throws Exception {

        try
        {
            String id = null;

            String userId = exchange.getProperty("group_name").toString();
            String groupName = exchange.getProperty("user_id").toString();

            if(exchange.getIn().hasHeaders()) {
                System.out.println("MessageID:" + exchange.getIn().getMessageId());
                id = exchange.getIn().getHeader("DocUUID").toString().isEmpty() ? exchange.getIn().getMessageId() : exchange.getIn().getHeader("DocUUID").toString();

            }

            Map<String,Object> headers = new HashMap<String,Object>();
            headers.put("auditId", id);
            headers.put("user", userId);
            headers.put("groupName", groupName);

            exchange.getOut().setBody(exchange.getIn().getBody());
            exchange.getOut().setHeaders(headers);

        }
        catch (Exception e)
        {
            throw  new Exception( "Failed to send message");
        }
    }
}
