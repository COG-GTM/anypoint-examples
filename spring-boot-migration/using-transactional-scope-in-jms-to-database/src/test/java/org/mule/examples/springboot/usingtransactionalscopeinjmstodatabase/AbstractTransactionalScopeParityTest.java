package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.MessagingParityAssertions;
import org.mule.examples.springboot.parity.MessagingParityFixture;
import org.mule.examples.springboot.parity.MessagingParityFixtures;
import org.mule.examples.springboot.parity.ParityContainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messaging + database parity for flow transactionsFlow1 against ActiveMQ Classic and MySQL 5.7 (Testcontainers).
 * Each fixture publishes XML orders to queue "in", waits for the expected deliveries, then checks the delivery
 * counts/outcomes, the committed rows of {@code orders}, how many inserts were executed (AUTO_INCREMENT moves even on
 * rollback), and what ended up on "out", "in" and ActiveMQ.DLQ.
 */
@SpringBootTest(properties = {"spring.config.import=optional:classpath:/parity/parity-defaults.yml",
        "orders.init-schema=always"})
@ActiveProfiles("parity")
// subclasses use different properties; a cached context's listener would otherwise steal messages from queue "in"
@DirtiesContext
abstract class AbstractTransactionalScopeParityTest {

    static final String ROOT = "parity/using-transactional-scope-in-jms-to-database/";
    static final GenericContainer<?> ACTIVEMQ = ParityContainers.activeMq();
    static final MySQLContainer<?> MYSQL = ParityContainers.mysql();

    static {
        ACTIVEMQ.start();
        MYSQL.start();
    }

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("activemq.broker-url", () -> "tcp://" + ACTIVEMQ.getHost() + ":" + ACTIVEMQ.getMappedPort(61616));
        // same shape as the Mule test's jdbc.url (credentials in the URL)
        registry.add("jdbc.url", () -> MYSQL.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&user="
                + MYSQL.getUsername() + "&password=" + MYSQL.getPassword());
    }

    @Autowired
    JmsTemplate jms;
    @Autowired
    JdbcClient jdbc;
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
        JmsCapture.drain(jms, "out", 200);
        jdbc.sql("delete from orders").update();
        attempts.clear();
        long autoIncrementBefore = autoIncrement();

        for (MessagingParityFixture.OutboundMessage m : fixture.send()) {
            jms.convertAndSend(new ActiveMQQueue(m.destination().replaceFirst("^queue:", "")), m.bodyText());
        }

        JsonNode expectedAttempts = state.path("deliveryAttempts");
        long deadline = System.currentTimeMillis() + state.path("timeoutMillis").asLong(60_000);
        while (attempts.all().size() < expectedAttempts.size() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        // give a broken implementation the chance to deliver/insert/publish more than expected
        Thread.sleep(state.path("settleMillis").asLong(3_000));

        List<String> want = new ArrayList<>();
        expectedAttempts.forEach(a -> want.add(a.path("deliveryCount").asInt() + ":" + a.path("outcome").asText()));
        List<String> got = attempts.all().stream().map(a -> a.deliveryCount() + ":" + a.outcome()).toList();
        assertThat(got).as("[%s] delivery attempts (JMSXDeliveryCount:outcome)", fixture.name()).isEqualTo(want);

        List<Map<String, Object>> wantRows = new ArrayList<>();
        state.path("ordersRows").forEach(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            r.fields().forEachRemaining(e -> row.put(e.getKey(), e.getValue().asInt()));
            wantRows.add(row);
        });
        List<Map<String, Object>> gotRows = jdbc.sql("select item_id, item_units, customer_id from orders order by id")
                .query((rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("item_id", rs.getInt(1));
                    row.put("item_units", rs.getInt(2));
                    row.put("customer_id", rs.getInt(3));
                    return row;
                }).list();
        assertThat(gotRows).as("[%s] committed rows in orders", fixture.name()).isEqualTo(wantRows);

        if (state.has("insertsExecuted")) {
            assertThat(autoIncrement() - autoIncrementBefore)
                    .as("[%s] inserts executed (committed or rolled back), from orders AUTO_INCREMENT", fixture.name())
                    .isEqualTo(state.path("insertsExecuted").asLong());
        }

        for (MessagingParityFixture.MessageExpectation expected : fixture.expectedMessages()) {
            String queue = expected.destination().replaceFirst("^queue:", "");
            MessagingParityAssertions.assertMessages(fixture, expected,
                    JmsCapture.drain(jms, queue, expected.count() > 0 ? 3000 : 500));
        }
    }

    private long autoIncrement() {
        return jdbc.sql("select auto_increment from information_schema.tables where table_schema = database() and table_name = 'orders'")
                .query(Long.class).single();
    }
}
