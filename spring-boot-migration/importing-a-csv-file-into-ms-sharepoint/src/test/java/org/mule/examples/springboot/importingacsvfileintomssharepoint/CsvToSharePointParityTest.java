package org.mule.examples.springboot.importingacsvfileintomssharepoint;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * File-triggered parity against a WireMock SharePoint site. Each fixture's {@code request.body} is dropped into the
 * input directory as {@code request.headers.File-Name}; {@code downstream} stubs the SharePoint REST API,
 * {@code expectedOutbound} lists the calls the Mule connector makes (folder-create then file-add, nothing else), and
 * {@code expected} describes the file left in the move-to directory.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
class CsvToSharePointParityTest {

    static final String FIXTURES = "parity/importing-a-csv-file-into-ms-sharepoint";
    static final WireMockParitySupport SHAREPOINT = new WireMockParitySupport().start();
    static final Path INPUT_DIR = tempDir("sp-input");
    static final Path OUTPUT_DIR = tempDir("sp-output");
    static final Path STAGING_DIR = tempDir("sp-staging");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("Sharepoint.SiteUrl", SHAREPOINT::baseUrl);
        registry.add("file.input-dir", INPUT_DIR::toString);
        registry.add("file.move-to-dir", OUTPUT_DIR::toString);
    }

    @AfterAll
    static void stop() {
        SHAREPOINT.stop();
    }

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load(FIXTURES).stream().map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    private void runCase(ParityFixture fixture) throws Exception {
        SHAREPOINT.reset();
        SHAREPOINT.stub(fixture.downstream());
        String fileName = fixture.request().headers().get("File-Name");
        Path staged = STAGING_DIR.resolve(fileName);
        Files.writeString(staged, ParityFixtures.bodyText(fixture.request().body()), StandardCharsets.UTF_8);
        Path input = INPUT_DIR.resolve(fileName);
        Files.move(staged, input, StandardCopyOption.ATOMIC_MOVE);

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(100))
                .until(() -> SHAREPOINT.verifyOutbound(fixture.expectedOutbound()).isEmpty());
        Thread.sleep(300); // let any (unexpected) extra downstream call surface
        assertThat(SHAREPOINT.verifyOutbound(fixture.expectedOutbound()))
                .as("[%s] SharePoint calls", fixture.name()).isEmpty();

        Path moved = OUTPUT_DIR.resolve(fileName);
        byte[] movedContent = Files.exists(moved) ? Files.readAllBytes(moved) : new byte[0];
        Map<String, List<String>> facts = Map.of(
                "File-Name", List.of(Files.exists(moved) ? fileName : ""),
                "Input-File-Remaining", List.of(String.valueOf(Files.exists(input))));
        FixtureExtras.assertSideEffect(fixture, "moved input file",
                FixtureExtras.raw(FIXTURES, fixture.name()).path("expected"), facts, movedContent);
    }

    private static Path tempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
