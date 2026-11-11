package org.mule.examples.springboot.parity.sample;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mule.examples.springboot.parity.AbstractWireMockParityTest;
import org.mule.examples.springboot.parity.BodyFormat;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates capture-and-compare mode without a real Mule runtime: a WireMock server replays
 * the recorded Mule responses (the fixtures) on its own port and is used as
 * {@code parity.mule.base-url}. Each case is therefore sent to both "Mule" and Spring Boot, both
 * are checked against the fixture and against each other, and the Mule capture is recorded.
 */
class MuleCaptureModeParityTest extends AbstractWireMockParityTest {

    private static final String FIXTURES = "parity/sample";
    private static final Path RECORDINGS = Path.of("target", "parity-recordings", "sample");
    private static WireMockServer fakeMule;

    @BeforeAll
    static void startFakeMule() {
        fakeMule = new WireMockServer(wireMockConfig().dynamicPort());
        fakeMule.start();
        for (ParityFixture f : ParityFixtures.load(FIXTURES)) {
            ResponseDefinitionBuilder response = WireMock.aResponse().withStatus(f.expected().status());
            if (f.expected().bodyFormat() == BodyFormat.JSON) {
                response.withHeader("Content-Type", "application/json");
            } else if (f.expected().bodyFormat() == BodyFormat.XML) {
                response.withHeader("Content-Type", "application/xml");
            }
            String body = ParityFixtures.bodyText(f.expected().body());
            if (body != null) {
                response.withBody(body);
            }
            fakeMule.stubFor(WireMock.request(f.request().method(), WireMock.urlPathEqualTo(f.request().path()))
                    .willReturn(response));
        }
    }

    @AfterAll
    static void stopFakeMule() {
        fakeMule.stop();
    }

    @Override
    protected String fixtureDirectory() {
        return FIXTURES;
    }

    @Override
    protected Optional<String> muleBaseUrl() {
        return Optional.of(fakeMule.baseUrl());
    }

    @Override
    protected boolean recording() {
        return true;
    }

    @Override
    protected Path recordingDirectory() {
        return RECORDINGS;
    }

    @Override
    protected void runCase(ParityFixture fixture) {
        super.runCase(fixture);
        assertThat(Files.exists(RECORDINGS.resolve(fixture.name() + ".json")))
                .as("Mule capture recorded for %s", fixture.name()).isTrue();
    }
}
