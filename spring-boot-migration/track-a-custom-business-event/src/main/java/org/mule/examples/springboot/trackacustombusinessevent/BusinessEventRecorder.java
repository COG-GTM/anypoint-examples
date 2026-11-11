package org.mule.examples.springboot.trackacustombusinessevent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Replaces the Insight business-event store: logs every {@link BusinessEvent}, keeps the most recent
 * {@value #CAPACITY} in memory for inspection
 * and counts it in Micrometer as {@code business.events{event=<name>}}.
 */
@Component
public class BusinessEventRecorder {

    static final String METER = "business.events";
    static final int CAPACITY = 1000;

    private static final Logger log = LoggerFactory.getLogger(BusinessEventRecorder.class);

    private final Deque<BusinessEvent> events = new ArrayDeque<>();
    private final MeterRegistry meterRegistry;

    public BusinessEventRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void on(BusinessEvent event) {
        synchronized (events) {
            if (events.size() == CAPACITY) {
                events.removeFirst();
            }
            events.addLast(event);
        }
        meterRegistry.counter(METER, "event", event.name()).increment();
        log.info("Business event {} [transaction {}]: {}", event.name(), event.transactionId(), event.metadata());
    }

    public List<BusinessEvent> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    public void clear() {
        synchronized (events) {
            events.clear();
        }
    }
}
