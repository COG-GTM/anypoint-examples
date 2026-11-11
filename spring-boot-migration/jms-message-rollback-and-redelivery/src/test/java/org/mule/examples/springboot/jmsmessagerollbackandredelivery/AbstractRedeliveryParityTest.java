package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.MessagingParityAssertions;
import org.mule.examples.springboot.parity.MessagingParityAssertions.CapturedMessage;
import org.mule.examples.springboot.parity.MessagingParityFixture;
import org.mule.examples.springboot.parity.MessagingParityFixtures;
import org.mule.examples.springboot.parity.ParityContainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messaging parity for flow JMSRedeliver against ActiveMQ Classic (Testcontainers). Each fixture publishes to
 * queue "in", waits for the expected deliveries, then checks the delivery counts/outcomes, the delays between
 * redeliveries, and what ended up on topic1, on queue "in" and on ActiveMQ.DLQ.
 */
@SpringBootTest(properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
// each subclass uses different properties; a cached context's listener would otherwise steal messages from queue "in"
@DirtiesContext
abstract class AbstractRedeliveryParityTest {

    static final String ROOT = "parity/jms-message-rollback-and-redelivery/";
    static final GenericContainer<?> ACTIVEMQ = ParityContainers.activeMq();
    /** Allowed scheduling jitter when checking ActiveMQ redelivery delays. */
    static final long DELAY_TOLERANCE_MILLIS = 300;

    static {
        ACTIVEMQ.start();
    }

    @DynamicPropertySource
    static void broker(DynamicPropertyRegistry registry) {
        registry.add("activemq.broker-url", () -> "tcp://" + ACTIVEMQ.getHost() + ":" + ACTIVEMQ.getMappedPort(61616));
    }

    @Autowired
    JmsTemplate jms;
    @Autowired
    ConnectionFactory connectionFactory;
    @Autowired
    DeliveryAttempts attempts;

    protected abstract String fixtureSubdirectory();

    @TestFactory
    Stream<DynamicTest> parity() {
        return MessagingParityFixtures.load(ROOT + fixtureSubdirectory()).stream()
                .map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    void runCase(MessagingParityFixture fixture) throws Exception {
        JsonNode state = fixture.expectedState();
        JmsCapture.drain(jms, "ActiveMQ.DLQ", 200);
        attempts.clear();
        List<CapturedMessage> topicMessages = new CopyOnWriteArrayList<>();
        try (Connection connection = connectionFactory.createConnection()) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createTopic(topicName(fixture)));
            consumer.setMessageListener(m -> topicMessages.add(JmsCapture.toCaptured(m)));
            connection.start();

            for (MessagingParityFixture.OutboundMessage m : fixture.send()) {
                jms.convertAndSend(new ActiveMQQueue(m.destination().replaceFirst("^queue:", "")), m.bodyText());
            }

            JsonNode expectedAttempts = state.path("deliveryAttempts");
            long deadline = System.currentTimeMillis() + state.path("timeoutMillis").asLong(60_000);
            while (attempts.all().size() < expectedAttempts.size() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }
            // give a broken implementation the chance to deliver/publish more than expected
            Thread.sleep(state.path("settleMillis").asLong(3_000));

            assertAttempts(fixture, expectedAttempts);
            assertDelays(fixture, state.path("minRedeliveryDelaysMillis"));
            for (MessagingParityFixture.MessageExpectation expected : fixture.expectedMessages()) {
                MessagingParityAssertions.assertMessages(fixture, expected, capture(expected, topicMessages));
            }
        }
    }

    private String topicName(MessagingParityFixture fixture) {
        return fixture.expectedMessages().stream().map(MessagingParityFixture.MessageExpectation::destination)
                .filter(d -> d.startsWith("topic:")).findFirst().orElse("topic:topic1").substring("topic:".length());
    }

    private List<CapturedMessage> capture(MessagingParityFixture.MessageExpectation expected, List<CapturedMessage> topic) {
        String destination = expected.destination();
        if (destination.startsWith("topic:")) {
            return new ArrayList<>(topic);
        }
        return JmsCapture.drain(jms, destination.replaceFirst("^queue:", ""), expected.count() > 0 ? 3000 : 500);
    }

    private void assertAttempts(MessagingParityFixture fixture, JsonNode expected) {
        List<String> want = new ArrayList<>();
        expected.forEach(a -> want.add(a.path("deliveryCount").asInt() + ":" + a.path("outcome").asText()));
        List<String> got = attempts.all().stream().map(a -> a.deliveryCount() + ":" + a.outcome()).toList();
        assertThat(got).as("[%s] delivery attempts (JMSXDeliveryCount:outcome)", fixture.name()).isEqualTo(want);
    }

    private void assertDelays(MessagingParityFixture fixture, JsonNode minDelays) {
        List<DeliveryAttempts.Attempt> all = attempts.all();
        for (int i = 0; i < minDelays.size() && i + 1 < all.size(); i++) {
            long gap = all.get(i + 1).timestampMillis() - all.get(i).timestampMillis();
            assertThat(gap).as("[%s] delay before delivery %d", fixture.name(), i + 2)
                    .isGreaterThanOrEqualTo(minDelays.get(i).asLong() - DELAY_TOLERANCE_MILLIS);
        }
    }
}
