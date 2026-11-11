package org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing;

import org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing.salesforce.SalesforcePasswordSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mule.examples.springboot.parity.BodyFormat;
import org.mule.examples.springboot.parity.ParityComparator;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Poll-triggered parity: each fixture is one run of {@code triggerFlow}. {@code given.watermark} seeds
 * the persisted watermark (null = first run); the SOQL sent to WireMock must match {@code expectedOutbound}
 * and {@code expected.body} holds the watermark after the poll and the batch step's log lines.
 */
@SpringBootTest(properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
@ExtendWith(OutputCaptureExtension.class)
class WatermarkingParityTest {

    static final String FIXTURES = "parity/salesforce-data-synchronization-using-watermarking-and-batch-processing";
    static final String LOG_PREFIX = "Contact recently modified: ";
    static final WireMockParitySupport DOWNSTREAM = new WireMockParitySupport().start();
    static final Map<String, JsonNode> GIVEN = SalesforceParitySupport.given(FIXTURES);
    static final ObjectMapper JSON = new ObjectMapper();

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

    @TestFactory
    Stream<DynamicTest> parity(CapturedOutput output) {
        return ParityFixtures.load(FIXTURES).stream().map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f, output)));
    }

    void runCase(ParityFixture fixture, CapturedOutput output) throws Exception {
        JsonNode given = GIVEN.get(fixture.name());
        if (given.path("watermark").isTextual()) {
            watermarks.put(TriggerFlow.WATERMARK, given.path("watermark").asText());
        } else {
            watermarks.clear(TriggerFlow.WATERMARK);
        }
        session.invalidate();
        SalesforceParitySupport.stub(DOWNSTREAM, fixture);
        int logStart = output.getOut().length();

        triggerFlow.poll();

        assertThat(DOWNSTREAM.verifyOutbound(fixture.expectedOutbound())).isEmpty();
        ObjectNode actual = JSON.createObjectNode();
        actual.put("watermark", watermarks.get(TriggerFlow.WATERMARK).orElse(null));
        SalesforceParitySupport.lines(output.getOut().substring(logStart), LOG_PREFIX).forEach(actual.putArray("log")::add);
        if (!actual.has("log")) {
            actual.putArray("log");
        }
        assertThat(ParityComparator.compareBody(BodyFormat.JSON, ParityFixtures.bodyText(fixture.expected().body()),
                actual.toString(), fixture.expected().jsonCompareMode(), fixture.expected().ignorePaths())).isEmpty();
    }
}
