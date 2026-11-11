package org.mule.examples.springboot.parity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.List;
import java.util.Map;

/**
 * Parity case for messaging apps (JMS, AMQP, WMQ). Lives in the same fixture directory and JSON
 * file as an HTTP {@link ParityFixture} when the flow has an HTTP entry point (extra fields are
 * ignored by {@link ParityFixtures}), or on its own for queue-triggered flows.
 *
 * <ul>
 *   <li>{@code send}: messages the test publishes to trigger the flow ({@code queue:in}, {@code topic:t}, ...).</li>
 *   <li>{@code expectedMessages}: messages the Mule flow emits; {@code count: 0} asserts none.</li>
 *   <li>{@code expectedState}: module-specific side effects (redelivery counts, DB rows, DLQ depth).</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessagingParityFixture(
        String name,
        String description,
        ParityFixture.Source source,
        List<OutboundMessage> send,
        List<MessageExpectation> expectedMessages,
        JsonNode expectedState) {

    public MessagingParityFixture {
        send = send == null ? List.of() : List.copyOf(send);
        expectedMessages = expectedMessages == null ? List.of() : List.copyOf(expectedMessages);
        expectedState = expectedState == null ? NullNode.getInstance() : expectedState;
    }

    @Override
    public String toString() {
        return name;
    }

    /** A message published by the test. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutboundMessage(String destination, JsonNode body, String bodyFile, Map<String, String> properties) {
        public OutboundMessage {
            properties = properties == null ? Map.of() : Map.copyOf(properties);
        }

        public String bodyText() {
            return ParityFixtures.bodyText(body);
        }
    }

    /**
     * A message the flow must emit. {@code type} is the transport message type (e.g. {@code TextMessage});
     * property values follow the header rules of {@link ParityComparator} (exact, {@code ~regex}, {@code !absent}).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageExpectation(
            String destination,
            Integer count,
            String type,
            BodyFormat bodyFormat,
            JsonNode body,
            String bodyFile,
            String jsonCompareMode,
            List<String> ignorePaths,
            Map<String, String> properties) {
        public MessageExpectation {
            count = count == null ? 1 : count;
            bodyFormat = bodyFormat == null ? BodyFormat.IGNORE : bodyFormat;
            jsonCompareMode = jsonCompareMode == null ? "STRICT" : jsonCompareMode;
            ignorePaths = ignorePaths == null ? List.of() : List.copyOf(ignorePaths);
            properties = properties == null ? Map.of() : Map.copyOf(properties);
        }

        public String bodyText() {
            return ParityFixtures.bodyText(body);
        }
    }
}
