package org.mule.examples.springboot.webspheremq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/** Flow Output: read from "out" and publish on the {@code /services/wmqExample/dequeue} channel. */
@Component
@ConditionalOnProperty(name = "wmq.output.enabled", havingValue = "true", matchIfMissing = true)
public class OutputFlow {

    private final DequeueChannel dequeue;

    OutputFlow(DequeueChannel dequeue) {
        this.dequeue = dequeue;
    }

    @JmsListener(destination = "${wmq.out-queue}")
    public void output(String payload) {
        dequeue.publish(payload);
    }
}
