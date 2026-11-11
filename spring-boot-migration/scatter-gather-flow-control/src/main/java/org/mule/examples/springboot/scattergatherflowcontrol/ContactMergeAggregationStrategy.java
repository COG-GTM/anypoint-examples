package org.mule.examples.springboot.scattergatherflowcontrol;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;

/**
 * Port of {@code ContactMergeAggregationStrategy}: requires both routes, orders the results by route index
 * (the scatterer's sequence number) and merges A with B.
 */
public final class ContactMergeAggregationStrategy {

    private ContactMergeAggregationStrategy() {
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> aggregate(MessageGroup group) {
        List<Message<?>> routes = group.getMessages().stream()
                .sorted(Comparator.comparingInt(m -> new IntegrationMessageHeaderAccessor(m).getSequenceNumber()))
                .toList();
        if (routes.size() != 2) {
            throw new IllegalStateException("Data from at least one source was not able to be obtained correctly.");
        }
        return ContactMerge.mergeList((List<Map<String, String>>) routes.get(0).getPayload(),
                (List<Map<String, String>>) routes.get(1).getPayload());
    }
}
