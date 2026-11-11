package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

/** Equivalent of Mule's {@code MessageRedeliveredException}: the connector's maxRedelivery was exceeded. */
public class MessageRedeliveredException extends RuntimeException {

    public MessageRedeliveredException(String messageId, int redeliveries, int maxRedelivery) {
        super("Message with id \"" + messageId + "\" has been redelivered " + redeliveries
                + " times, which exceeds the maxRedelivery setting of " + maxRedelivery + " on the connector");
    }
}
