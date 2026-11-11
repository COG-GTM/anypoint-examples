package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Bounded record of the most recent deliveries of queue "in" (JMSXDeliveryCount and outcome), for monitoring and the parity tests. */
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

    static final int CAPACITY = 1000;

    private final Deque<Attempt> attempts = new ArrayDeque<>();

    synchronized void record(String messageId, int deliveryCount, Outcome outcome) {
        if (attempts.size() == CAPACITY) {
            attempts.removeFirst();
        }
        attempts.addLast(new Attempt(messageId, deliveryCount, outcome, System.currentTimeMillis()));
    }

    public synchronized List<Attempt> all() {
        return List.copyOf(attempts);
    }

    public synchronized void clear() {
        attempts.clear();
    }
}
