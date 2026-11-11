package org.mule.examples.springboot.trackacustombusinessevent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Replaces the Insight business-event store: logs every {@link BusinessEvent}, keeps it in memory for inspection
 * and counts it in Micrometer as {@code business.events{event=<name>}}.
 */
@Component
public class BusinessEventRecorder {

    static final String METER = "business.events";

    private static final Logger log = LoggerFactory.getLogger(BusinessEventRecorder.class);

    private final List<BusinessEvent> events = new CopyOnWriteArrayList<>();
    private final MeterRegistry meterRegistry;

    public BusinessEventRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void on(BusinessEvent event) {
        events.add(event);
        meterRegistry.counter(METER, "event", event.name()).increment();
        log.info("Business event {} [transaction {}]: {}", event.name(), event.transactionId(), event.metadata());
    }

    public List<BusinessEvent> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
