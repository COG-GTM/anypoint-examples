package org.mule.examples.springboot.scattergatherflowcontrol;

import java.util.List;
import java.util.Map;

/**
 * Gatherer output. Wrapped because a raw {@code List} returned by an aggregator output processor is treated by
 * Spring Integration as a collection of messages to emit.
 */
public record MergedContacts(List<Map<String, String>> contacts) {
}
