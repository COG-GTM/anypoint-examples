package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Record of every delivery the flow saw (JMSXDeliveryCount and outcome), for monitoring and the parity tests. */
@Component
public class DeliveryAttempts {

    public enum Outcome {
        /** No error: the topic message and the consumed message were committed together. */
        COMMITTED,
        /** rollback-exception-strategy (cause is MyException): the message goes back to the queue. */
        ROLLED_BACK,
        /** catch-exception-strategy (any other error): the error is logged and the transaction committed. */
        COMMITTED_AFTER_ERROR
    }

    public record Attempt(String messageId, int deliveryCount, Outcome outcome, long timestampMillis) {
    }

    private final List<Attempt> attempts = new CopyOnWriteArrayList<>();

    void record(String messageId, int deliveryCount, Outcome outcome) {
        attempts.add(new Attempt(messageId, deliveryCount, outcome, System.currentTimeMillis()));
    }

    public List<Attempt> all() {
        return List.copyOf(attempts);
    }

    public void clear() {
        attempts.clear();
    }
}
