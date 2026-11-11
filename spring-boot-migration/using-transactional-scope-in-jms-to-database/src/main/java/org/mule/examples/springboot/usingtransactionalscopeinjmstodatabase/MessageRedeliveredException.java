package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

/** Equivalent of org.mule.transport.jms.redelivery.MessageRedeliveredException. */
public class MessageRedeliveredException extends RuntimeException {

    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, String endpoint) {
        super("Message with id \"" + messageId + "\" has been redelivered " + redeliveryCount + " times on endpoint \""
                + endpoint + "\", which exceeds the maxRedelivery setting of " + maxRedelivery + " on the connector");
    }
}
