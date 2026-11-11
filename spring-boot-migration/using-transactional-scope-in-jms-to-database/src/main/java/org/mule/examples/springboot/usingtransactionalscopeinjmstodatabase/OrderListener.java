package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/** Flow transactionsFlow1: {@code jms:inbound-endpoint queue="in"} → xml-to-object → logger → transactional scope. */
@Component
public class OrderListener {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);
    private static final XmlMapper XML = new XmlMapper();

    private final OrderTransactionalScope scope;
    private final DeliveryAttempts attempts;
    private final int maxRedelivery;
    private final String inQueue;

    OrderListener(OrderTransactionalScope scope, DeliveryAttempts attempts,
                  @Value("${jms.max-redelivery}") int maxRedelivery, @Value("${jms.in-queue}") String inQueue) {
        this.scope = scope;
        this.attempts = attempts;
        this.maxRedelivery = maxRedelivery;
        this.inQueue = inQueue;
    }

    @JmsListener(destination = "${jms.in-queue}", containerFactory = "transactedListenerFactory")
    public void onMessage(Message message) throws Exception {
        String id = message.getJMSMessageID();
        int deliveryCount = message.getIntProperty("JMSXDeliveryCount");
        int redeliveryCount = deliveryCount - 1;
        if (maxRedelivery >= 0 && redeliveryCount > maxRedelivery) {
            MessageRedeliveredException e = new MessageRedeliveredException(id, redeliveryCount, maxRedelivery, "jms://" + inQueue);
            log.error(e.getMessage() + ". Message payload is of type: " + message.getClass().getSimpleName(), e);
            attempts.record(id, deliveryCount, DeliveryAttempts.Outcome.COMMITTED_AFTER_ERROR);
            return;
        }
        try {
            Order order = XML.readValue(((TextMessage) message).getText(), Order.class);
            log.info("XXXXXXX order: {}", order);
            scope.process(order);
            attempts.record(id, deliveryCount, DeliveryAttempts.Outcome.COMMITTED);
        } catch (Exception e) {
            log.info("Transactional scope rolled back on delivery {} of message {}: {}", deliveryCount, id, e.toString());
            attempts.record(id, deliveryCount, DeliveryAttempts.Outcome.ROLLED_BACK);
            throw e;
        }
    }
}
