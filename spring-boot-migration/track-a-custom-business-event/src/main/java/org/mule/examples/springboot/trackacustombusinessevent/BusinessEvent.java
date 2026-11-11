package org.mule.examples.springboot.trackacustombusinessevent;

import java.util.Map;

/** A {@code tracking:custom-event}: event name, its meta-data and the flow's {@code tracking:transaction} id. */
public record BusinessEvent(String name, String transactionId, Map<String, String> metadata) {
}
