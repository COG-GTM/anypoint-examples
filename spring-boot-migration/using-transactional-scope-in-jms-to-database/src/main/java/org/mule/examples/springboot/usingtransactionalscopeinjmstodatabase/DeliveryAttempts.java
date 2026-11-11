package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Record of every delivery of queue "in" (JMSXDeliveryCount and outcome), for monitoring and the parity tests. */
@Component
public class DeliveryAttempts {

    public enum Outcome {
        /** Insert and the "out" message committed together. */
        COMMITTED,
        /** The transactional scope failed: insert and "out" message rolled back, the message is redelivered. */
        ROLLED_BACK,
        /** maxRedelivery exceeded: the error is logged and the message is consumed without running the scope. */
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
