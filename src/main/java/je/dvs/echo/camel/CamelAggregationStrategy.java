package je.dvs.echo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

public class CamelAggregationStrategy implements AggregationStrategy
{

   @Autowired
   ObjectMapper objectMapper;


    public CamelAggregationStrategy(){
        super();
    }

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange){
        Message newIn = newExchange.getIn();
        Object newBody = newIn.getBody();
        ArrayList list = null;
        if (oldExchange == null) {
            list = new ArrayList();
            list.add(newBody);
            newIn.setBody(list);
            return newExchange;
        } else {
            Message in = oldExchange.getIn();
            list = in.getBody(ArrayList.class);
            list.add(newBody);
            return oldExchange;
        }
    }





}

