package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

import org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing.salesforce.SalesforcePasswordSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.BodyFormat;
import org.mule.examples.springboot.parity.ParityComparator;
import org.mule.examples.springboot.parity.ParityContainers;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Poll-triggered parity against a real MySQL 5.7 (Testcontainers, schema from the Mule app's {@code contact.sql})
 * and the WireMock Salesforce API. {@code given} seeds the clock, the persisted watermark and the {@code contact}
 * table; {@code expected.body} is the watermark and the {@code contact} rows after one run of {@code triggerFlow}.
 */
@SpringBootTest(properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
class SalesforceToMySqlParityTest {

    static final String FIXTURES = "parity/salesforce-to-MySQL-DB-using-Batch-Processing";
    static final WireMockParitySupport DOWNSTREAM = new WireMockParitySupport().start();
    static final Map<String, JsonNode> GIVEN = SalesforceParitySupport.given(FIXTURES);
    static final ObjectMapper JSON = new ObjectMapper();

    @TestConfiguration(proxyBeanMethods = false)
    static class Infrastructure {

        @Bean
        @ServiceConnection
        MySQLContainer<?> mysql() {
            return ParityContainers.mysql();
        }

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("parity.downstream.base-url", DOWNSTREAM::baseUrl);
    }

    @Autowired
    SalesforcePasswordSession session;

    @Autowired
    TriggerFlow triggerFlow;

    @Autowired
    WatermarkStore watermarks;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load(FIXTURES).stream().map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    void runCase(ParityFixture fixture) throws Exception {
        JsonNode given = GIVEN.get(fixture.name());
        clock.set(Instant.parse(given.path("now").asText()));
        if (given.path("watermark").isTextual()) {
            watermarks.put(TriggerFlow.WATERMARK, given.path("watermark").asText());
        } else {
            watermarks.clear(TriggerFlow.WATERMARK);
        }
        jdbc.update("DELETE FROM contact");
        given.path("contacts").forEach(c -> jdbc.update("INSERT INTO contact (first_name, last_name, email) VALUES (?,?,?)",
                c.path("first_name").asText(null), c.path("last_name").asText(null), c.path("email").asText()));
        session.invalidate();
        SalesforceParitySupport.stub(DOWNSTREAM, fixture);

        triggerFlow.poll();

        assertThat(DOWNSTREAM.verifyOutbound(fixture.expectedOutbound())).isEmpty();
        ObjectNode actual = JSON.createObjectNode();
        actual.put("watermark", watermarks.get(TriggerFlow.WATERMARK).orElse(null));
        ArrayNode contacts = actual.putArray("contacts");
        jdbc.queryForList("SELECT email, first_name, last_name FROM contact ORDER BY email").forEach(row -> {
            ObjectNode c = contacts.addObject();
            row.forEach((k, v) -> c.put(k.toLowerCase(), v == null ? null : v.toString()));
        });
        assertThat(ParityComparator.compareBody(BodyFormat.JSON, ParityFixtures.bodyText(fixture.expected().body()),
                actual.toString(), fixture.expected().jsonCompareMode(), fixture.expected().ignorePaths())).isEmpty();
    }
}
