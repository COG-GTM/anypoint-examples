package org.mule.examples.springboot.trackacustombusinessevent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * custom-business-eventsFlow1: JSON to HashMap, {@code tracking:custom-event name="Price"}, the JRuby discount
 * transformer and {@code object-to-string-transformer}.
 */
@RestController
public class CustomBusinessEventsController {

    static final String TRANSACTION_ID = "custom-business-event-example-transaction";
    private static final String ITEM_NAME = "item name";
    private static final String ITEM_UNITS = "item units";
    private static final String PRICE = "item price per unit";

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    public CustomBusinessEventsController(ObjectMapper objectMapper, ApplicationEventPublisher events) {
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @RequestMapping(path = "/customBusinessEvents", produces = MediaType.TEXT_PLAIN_VALUE)
    public String track(@RequestBody String body) throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(body, HashMap.class);

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(ITEM_NAME, String.valueOf(payload.get(ITEM_NAME)));
        metadata.put(ITEM_UNITS, String.valueOf(payload.get(ITEM_UNITS)));
        metadata.put(PRICE, String.valueOf(payload.get(PRICE)));
        events.publishEvent(new BusinessEvent("Price", TRANSACTION_ID, metadata));

        double discount = discountFor(payload.get(ITEM_NAME));
        double discounted = ((Number) payload.get(PRICE)).doubleValue() * (1 - discount);
        return Double.toString(discounted);
    }

    private static double discountFor(Object itemName) {
        if (itemName instanceof String name) {
            switch (name) {
                case "shoes":
                    return 0.15;
                case "jeans":
                    return 0.2;
                case "jackets":
                    return 0.3;
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("No discount defined for item name " + itemName);
    }
}
