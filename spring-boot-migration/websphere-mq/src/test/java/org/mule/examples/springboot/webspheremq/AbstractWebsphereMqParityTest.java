package org.mule.examples.springboot.webspheremq;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.MessagingParityAssertions;
import org.mule.examples.springboot.parity.MessagingParityAssertions.CapturedMessage;
import org.mule.examples.springboot.parity.MessagingParityFixture;
import org.mule.examples.springboot.parity.MessagingParityFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parity for flows Input / MessageProcessor / Output against a real IBM MQ queue manager (Testcontainers). Each
 * fixture subscribes to the dequeue channel (SSE), sends its messages either through the enqueue channel
 * ({@code http:/services/wmqExample/enqueue}) or straight onto a WMQ queue ({@code queue:in}), then checks what
 * arrived on the dequeue channel and on the WMQ queues, and that the processor's 15 s wait happened.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
// subclasses use different properties; a cached context's listeners would otherwise consume the test's messages
@DirtiesContext
abstract class AbstractWebsphereMqParityTest {

    static final String ROOT = "parity/websphere-mq/";
    static final String DEQUEUE_CHANNEL = "sse:/services/wmqExample/dequeue";
    static final long TIMING_TOLERANCE_MILLIS = 500;
    static final IbmMqContainer MQ = new IbmMqContainer();

    static {
        MQ.start();
    }

    @DynamicPropertySource
    static void mq(DynamicPropertyRegistry registry) {
        registry.add("wmq.queue", () -> IbmMqContainer.QUEUE_MANAGER);
        registry.add("wmq.channel", () -> IbmMqContainer.CHANNEL);
        registry.add("ibm.mq.conn-name", MQ::connName);
        registry.add("wmq.username", () -> IbmMqContainer.USER);
        registry.add("wmq.password", () -> MQ.password);
    }

    @LocalServerPort
    int port;
    @Autowired
    JmsTemplate jms;

    final HttpClient http = HttpClient.newHttpClient();

    protected abstract String fixtureSubdirectory();

    @TestFactory
    Stream<DynamicTest> parity() {
        return MessagingParityFixtures.load(ROOT + fixtureSubdirectory()).stream()
                .map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    void runCase(MessagingParityFixture fixture) throws Exception {
        JsonNode state = fixture.expectedState();
        drain("in", 200);
        drain("out", 200);
        List<String> dequeued = new CopyOnWriteArrayList<>();
        List<Long> dequeuedAt = new CopyOnWriteArrayList<>();
        CountDownLatch subscribed = new CountDownLatch(1);
        CompletableFuture<HttpResponse<Stream<String>>> sse = http.sendAsync(
                HttpRequest.newBuilder(uri("/services/wmqExample/dequeue")).header("Accept", "text/event-stream").build(),
                HttpResponse.BodyHandlers.ofLines());
        sse.thenAccept(r -> r.body().forEach(line -> {
            if (line.startsWith(":")) {
                subscribed.countDown();
            } else if (line.startsWith("data:")) {
                dequeued.add(line.substring(5));
                dequeuedAt.add(System.currentTimeMillis());
            }
        }));
        try {
            assertThat(subscribed.await(10, TimeUnit.SECONDS)).as("dequeue channel subscription").isTrue();

            long sentAt = System.currentTimeMillis();
            for (MessagingParityFixture.OutboundMessage m : fixture.send()) {
                send(fixture, m, state);
            }

            int expectedDequeued = fixture.expectedMessages().stream()
                    .filter(e -> e.destination().equals(DEQUEUE_CHANNEL)).mapToInt(MessagingParityFixture.MessageExpectation::count).sum();
            long deadline = System.currentTimeMillis() + state.path("timeoutMillis").asLong(60_000);
            while (dequeued.size() < expectedDequeued && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }

            for (MessagingParityFixture.MessageExpectation expected : fixture.expectedMessages()) {
                List<CapturedMessage> captured;
                if (expected.destination().equals(DEQUEUE_CHANNEL)) {
                    Thread.sleep(state.path("settleMillis").asLong(2_000));
                    captured = dequeued.stream().map(d -> new CapturedMessage("SSE", d, Map.of())).toList();
                    if (!dequeuedAt.isEmpty() && state.has("minProcessingMillis")) {
                        assertThat(dequeuedAt.get(0) - sentAt).as("[%s] time to dequeue", fixture.name())
                                .isGreaterThanOrEqualTo(state.path("minProcessingMillis").asLong() - TIMING_TOLERANCE_MILLIS);
                    }
                } else {
                    String queue = expected.destination().replaceFirst("^queue:", "");
                    long firstWait = expected.count() > 0 ? Math.max(0, deadline - System.currentTimeMillis()) : 1000;
                    captured = drain(queue, firstWait);
                    if (expected.count() > 0 && state.has("minProcessingMillis")) {
                        assertThat(System.currentTimeMillis() - sentAt).as("[%s] time until %s", fixture.name(), queue)
                                .isGreaterThanOrEqualTo(state.path("minProcessingMillis").asLong() - TIMING_TOLERANCE_MILLIS);
                    }
                }
                MessagingParityAssertions.assertMessages(fixture, expected, captured);
            }
        } finally {
            sse.cancel(true);
        }
    }

    private void send(MessagingParityFixture fixture, MessagingParityFixture.OutboundMessage m, JsonNode state) throws Exception {
        String destination = m.destination();
        if (destination.startsWith("http:")) {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri(destination.substring(5)))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(m.bodyText(), StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).as("[%s] enqueue status", fixture.name())
                    .isEqualTo(state.path("enqueueStatus").asInt(200));
            if (state.has("enqueueBody")) {
                assertThat(response.body()).as("[%s] enqueue response body", fixture.name())
                        .isEqualTo(state.path("enqueueBody").asText());
            }
        } else {
            jms.convertAndSend(destination.replaceFirst("^queue:", ""), m.bodyText());
        }
    }

    private List<CapturedMessage> drain(String queue, long firstWaitMillis) {
        List<CapturedMessage> captured = new ArrayList<>();
        long wait = Math.max(1, firstWaitMillis);
        while (true) {
            jms.setReceiveTimeout(wait);
            Message m = jms.receive(queue);
            if (m == null) {
                return captured;
            }
            try {
                String body = m instanceof TextMessage t ? t.getText() : null;
                captured.add(new CapturedMessage(m instanceof TextMessage ? "TextMessage" : m.getClass().getSimpleName(), body, Map.of()));
            } catch (jakarta.jms.JMSException e) {
                throw new IllegalStateException(e);
            }
            wait = 500;
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

}
