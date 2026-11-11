package org.mule.examples.springboot.dataweavewithflowreflookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.AbstractWireMockParityTest;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Parity for a file-triggered flow. The Mule app has no HTTP listener, so instead of the HTTP harness each fixture's
 * {@code request} is a file drop: {@code method = "FILE"}, {@code path} = file name, {@code bodyFile} = its content.
 * The observable behaviour is the Salesforce call ({@code downstream} / {@code expectedOutbound}, verified with the
 * shared WireMock support, unmatched calls fail) and the file being moved from the input to the output folder.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("parity")
class DataweaveWithFlowreflookupFileParityTest {

    private static final WireMockParitySupport SALESFORCE = new WireMockParitySupport().start();
    private static final Path INPUT = tempDir("input");
    private static final Path OUTPUT = tempDir("output");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(AbstractWireMockParityTest.DOWNSTREAM_BASE_URL_PROPERTY, SALESFORCE::baseUrl);
        registry.add("input.dir", INPUT::toString);
        registry.add("output.dir", OUTPUT::toString);
        registry.add("polling.frequency", () -> "100");
    }

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load("parity/dataweave-with-flowreflookup").stream()
                .map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    private void runCase(ParityFixture fixture) throws IOException {
        assertThat(fixture.request().method()).isEqualTo("FILE");
        SALESFORCE.reset();
        SALESFORCE.stub(fixture.downstream());
        String fileName = fixture.request().path();
        Path staging = Files.createTempFile("parity", ".csv");
        Files.writeString(staging, ParityFixtures.bodyText(fixture.request().body()), StandardCharsets.UTF_8);
        Files.move(staging, INPUT.resolve(fileName));

        int expectedCalls = fixture.expectedOutbound().stream().mapToInt(ParityFixture.OutboundExpectation::count).sum();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(INPUT.resolve(fileName)).as("file consumed from input").doesNotExist();
            assertThat(OUTPUT.resolve(fileName)).as("file moved to output").exists();
            assertThat(SALESFORCE.server().getAllServeEvents()).hasSizeGreaterThanOrEqualTo(expectedCalls);
        });
        // give a misbehaving implementation the chance to make extra calls before verifying
        await().pollDelay(Duration.ofMillis(300)).until(() -> true);
        List<String> mismatches = SALESFORCE.verifyOutbound(fixture.expectedOutbound());
        assertThat(mismatches).as("[%s] Salesforce side effects", fixture.name()).isEmpty();
    }

    private static Path tempDir(String name) {
        try {
            return Files.createTempDirectory("dataweave-" + name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
