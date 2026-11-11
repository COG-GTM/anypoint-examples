package org.mule.examples.springboot.contentbasedrouting;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;

@MessagingGateway(defaultRequestChannel = ContentBasedRoutingFlowConfig.INPUT)
public interface GreetingGateway {

    /** Sends a {@link GreetingRequest}; the reply carries the greeting and the {@code language} header. */
    Message<String> greet(GreetingRequest request);
}
