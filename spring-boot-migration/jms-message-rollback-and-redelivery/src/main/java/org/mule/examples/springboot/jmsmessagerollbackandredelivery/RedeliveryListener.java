package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import jakarta.jms.Message;
import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Flow {@code JMSRedeliver}: transacted JMS inbound on queue "in" → choice on JMSXDeliveryCount
 * (== 5: log, otherwise throw MyException) → JMS outbound to topic "topic1" joining the transaction.
 * choice-exception-strategy: MyException → rollback (redelivery by ActiveMQ), anything else → log and commit.
 */
@Component
public class RedeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(RedeliveryListener.class);

    private final JmsTemplate jmsTemplate;
    private final DeliveryAttempts attempts;
    private final ActiveMQTopic topic;
    private final int maxRedelivery;
    private final int successDeliveryCount;

    public RedeliveryListener(JmsTemplate jmsTemplate, DeliveryAttempts attempts,
                              @Value("${jms.topic}") String topic,
                              @Value("${jms.max-redelivery}") int maxRedelivery,
                              @Value("${jms.success-delivery-count}") int successDeliveryCount) {
        this.jmsTemplate = jmsTemplate;
        this.attempts = attempts;
        this.topic = new ActiveMQTopic(topic);
        this.maxRedelivery = maxRedelivery;
        this.successDeliveryCount = successDeliveryCount;
    }

    @JmsListener(destination = "${jms.in-queue}", containerFactory = "transactedListenerFactory")
    public void onMessage(Message message) throws Exception {
        String id = message.getJMSMessageID();
        int deliveryCount = message.getIntProperty("JMSXDeliveryCount");
        try {
            if (maxRedelivery >= 0 && deliveryCount - 1 > maxRedelivery) {
                throw new MessageRedeliveredException(id, deliveryCount - 1, maxRedelivery);
            }
            if (deliveryCount == successDeliveryCount) {
                log.info("Transaction without errors");
            } else {
                throw new MyException("Delivery " + deliveryCount + " of message " + id);
            }
            // The template reuses the listener's transacted session (jms:transaction action="ALWAYS_JOIN").
            jmsTemplate.convertAndSend(topic, jmsTemplate.getMessageConverter().fromMessage(message));
            attempts.record(id, deliveryCount, DeliveryAttempts.Outcome.COMMITTED);
        } catch (Exception e) {
            if (causedBy(e, MyException.class)) {
                log.info("Entered rollback exception strategy. The message rolls back to its original state for reprocessing.");
                attempts.record(id, deliveryCount, DeliveryAttempts.Outcome.ROLLED_BACK);
                throw e;
            }
            log.info("Entered catch exception strategy. The transaction is commited. ({})", e.getMessage());
            attempts.record(id, deliveryCount, DeliveryAttempts.Outcome.COMMITTED_AFTER_ERROR);
        }
    }

    private static boolean causedBy(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }
}
