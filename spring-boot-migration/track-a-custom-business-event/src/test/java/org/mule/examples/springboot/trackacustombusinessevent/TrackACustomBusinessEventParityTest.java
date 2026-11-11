package org.mule.examples.springboot.trackacustombusinessevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Besides the HTTP response, each fixture's {@code expectedEvents} lists the business events the Mule flow emits
 * for that input; the Spring side must record the same events (name, transaction id, meta-data).
 */
class TrackACustomBusinessEventParityTest extends AbstractParityTest {

    private static final String DIR = "parity/track-a-custom-business-event";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private BusinessEventRecorder recorder;

    @Autowired
    private MeterRegistry meterRegistry;

    private double countBefore;

    @Override
    protected String fixtureDirectory() {
        return DIR;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        recorder.clear();
        countBefore = priceCount();
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        List<BusinessEvent> expected = expectedEvents(fixture);
        assertThat(recorder.events()).as("business events for %s", fixture.name()).isEqualTo(expected);
        assertThat(priceCount() - countBefore).as("Micrometer business.events{event=Price}")
                .isEqualTo(expected.stream().filter(e -> e.name().equals("Price")).count());
    }

    private double priceCount() {
        var counter = meterRegistry.find(BusinessEventRecorder.METER).tag("event", "Price").counter();
        return counter == null ? 0 : counter.count();
    }

    private static List<BusinessEvent> expectedEvents(ParityFixture fixture) {
        try (InputStream in = TrackACustomBusinessEventParityTest.class.getClassLoader()
                .getResourceAsStream(DIR + "/" + fixture.name() + ".json")) {
            assertThat(in).as("fixture file for %s", fixture.name()).isNotNull();
            JsonNode node = JSON.readTree(in).path("expectedEvents");
            assertThat(node.isArray()).as("%s declares expectedEvents", fixture.name()).isTrue();
            List<BusinessEvent> events = new ArrayList<>();
            for (JsonNode e : node) {
                Map<String, String> metadata = JSON.convertValue(e.get("metadata"), new TypeReference<>() { });
                events.add(new BusinessEvent(e.get("name").asText(), e.get("transactionId").asText(), metadata));
            }
            return events;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
