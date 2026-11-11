package org.mule.examples.springboot.filteringamessage;

import java.util.Map;

import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway(defaultRequestChannel = FilteringFlowConfig.INPUT)
public interface DiscountGateway {

    /** Returns the flow outcome: the granted message, or an empty string when the custom filter rejects the message. */
    String evaluate(Map<String, Object> request);
}
