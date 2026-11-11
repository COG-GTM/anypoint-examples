package org.mule.examples.springboot.parity;

import org.junit.jupiter.api.Test;
import org.mule.examples.springboot.parity.MessagingParityAssertions.CapturedMessage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingParityAssertionsTest {

    private final MessagingParityFixture fixture =
            MessagingParityFixtures.byName("parity/messaging-sample", "queue-message");

    @Test
    void loadsFixtureAndResolvesBodyFiles() {
        assertThat(fixture.send()).singleElement().satisfies(m -> {
            assertThat(m.destination()).isEqualTo("queue:in");
            assertThat(m.bodyText()).contains("\"item\": \"Shirt\"");
        });
        assertThat(fixture.expectedMessages().get(1).count()).isZero();
        assertThat(fixture.expectedState().path("deliveryAttempts").get(0).asInt()).isEqualTo(1);
    }

    @Test
    void matchingMessagePasses() {
        var expected = fixture.expectedMessages().get(0);
        var actual = new CapturedMessage("TextMessage", "{\"item\":\"Shirt\",\"id\":7}", Map.of("JMSXDeliveryCount", "1"));
        assertThat(MessagingParityAssertions.compare(expected, List.of(actual))).isEmpty();
    }

    @Test
    void reportsCountTypePropertyAndBodyMismatches() {
        var expected = fixture.expectedMessages().get(0);
        assertThat(MessagingParityAssertions.compare(expected, List.of()))
                .singleElement().asString().contains("expected 1 message(s) but captured 0");
        var wrong = new CapturedMessage("BytesMessage", "{\"item\":\"Hat\",\"id\":7}",
                Map.of("JMSXDeliveryCount", "2", "JMSCorrelationID", "x"));
        assertThat(MessagingParityAssertions.compare(expected, List.of(wrong)))
                .hasSize(4)
                .anyMatch(s -> s.contains("type"))
                .anyMatch(s -> s.contains("JMSXDeliveryCount"))
                .anyMatch(s -> s.contains("JMSCorrelationID"))
                .anyMatch(s -> s.contains("item"));
    }

    @Test
    void zeroCountRejectsUnexpectedMessage() {
        var none = fixture.expectedMessages().get(1);
        assertThat(MessagingParityAssertions.compare(none, List.of(new CapturedMessage("TextMessage", "x", null))))
                .singleElement().asString().contains("expected 0");
    }
}
