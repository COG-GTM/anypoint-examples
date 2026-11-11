package org.mule.examples.springboot.sendingjsondatatoaamqpqueue;

import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.MessagingParityAssertions;
import org.mule.examples.springboot.parity.MessagingParityAssertions.CapturedMessage;
import org.mule.examples.springboot.parity.MessagingParityFixture;
import org.mule.examples.springboot.parity.MessagingParityFixtures;
import org.mule.examples.springboot.parity.ParityContainers;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runs the json-to-rabbitmqFlow fixtures against the Spring Boot endpoint with a real RabbitMQ broker
 * (Testcontainers). Fixtures with {@code expectedState.replyWith} start a consumer on sales_queue that answers the
 * request-response call; the others read whatever was left on sales_queue.
 */
class SendingJsonDataToAAmqpQueueParityTest extends AbstractParityTest {

    static final String DIR = "parity/sending-json-data-to-a-amqp-queue";
    static final String QUEUE = "sales_queue";
    static final GenericContainer<?> RABBIT = ParityContainers.rabbitMq();

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void broker(DynamicPropertyRegistry registry) {
        registry.add("amqp.host", RABBIT::getHost);
        registry.add("amqp.port", () -> RABBIT.getMappedPort(5672));
    }

    @Autowired
    ConnectionFactory connectionFactory;
    @Autowired
    RabbitAdmin admin;

    private SimpleMessageListenerContainer replier;
    private final List<CapturedMessage> replied = new CopyOnWriteArrayList<>();

    @Override
    protected String fixtureDirectory() {
        return DIR;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        admin.purgeQueue(QUEUE, false);
        replied.clear();
        String replyWith = MessagingParityFixtures.byName(DIR, fixture.name()).expectedState().path("replyWith").asText(null);
        if (replyWith != null) {
            startReplier(replyWith);
        }
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        if (replier != null) {
            replier.stop();
            replier = null;
        }
        MessagingParityFixture messaging = MessagingParityFixtures.byName(DIR, fixture.name());
        boolean replied = messaging.expectedState().has("replyWith");
        for (MessagingParityFixture.MessageExpectation expected : messaging.expectedMessages()) {
            List<CapturedMessage> actual = replied ? this.replied : drain(expected.destination().replaceFirst("^queue:", ""));
            MessagingParityAssertions.assertMessages(messaging, expected, actual);
        }
    }

    private void startReplier(String replyWith) {
        RabbitTemplate replies = new RabbitTemplate(connectionFactory);
        replier = new SimpleMessageListenerContainer(connectionFactory);
        replier.setQueueNames(QUEUE);
        replier.setMessageListener((Message request) -> {
            replied.add(toCaptured(request));
            MessageProperties props = new MessageProperties();
            props.setCorrelationId(request.getMessageProperties().getCorrelationId());
            replies.send("", request.getMessageProperties().getReplyTo(),
                    new Message(replyWith.getBytes(StandardCharsets.UTF_8), props));
        });
        replier.start();
        long deadline = System.currentTimeMillis() + 10_000;
        while (consumers() < 1 && System.currentTimeMillis() < deadline) {
            sleep(100);
        }
    }

    private int consumers() {
        Properties p = admin.getQueueProperties(QUEUE);
        return p == null ? 0 : ((Number) p.get(RabbitAdmin.QUEUE_CONSUMER_COUNT)).intValue();
    }

    private List<CapturedMessage> drain(String queue) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        List<CapturedMessage> captured = new ArrayList<>();
        Message m = template.receive(queue, 3000);
        while (m != null) {
            captured.add(toCaptured(m));
            m = template.receive(queue, 500);
        }
        return captured;
    }

    private static CapturedMessage toCaptured(Message m) {
        MessageProperties p = m.getMessageProperties();
        Map<String, String> props = new java.util.LinkedHashMap<>();
        p.getHeaders().forEach((k, v) -> props.put(k, String.valueOf(v)));
        if (p.getReplyTo() != null) {
            props.put("replyTo", p.getReplyTo());
        }
        props.put("receivedExchange", String.valueOf(p.getReceivedExchange()));
        props.put("receivedRoutingKey", String.valueOf(p.getReceivedRoutingKey()));
        return new CapturedMessage("bytes", new String(m.getBody(), StandardCharsets.UTF_8), props);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
