package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Bounded record of the most recent deliveries the flow saw (JMSXDeliveryCount and outcome), for monitoring and the parity tests. */
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
