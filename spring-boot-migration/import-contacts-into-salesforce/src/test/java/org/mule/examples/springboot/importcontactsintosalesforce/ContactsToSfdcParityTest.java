package org.mule.examples.springboot.importcontactsintosalesforce;

import org.mule.examples.springboot.importcontactsintosalesforce.salesforce.SalesforcePasswordSession;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.BodyFormat;
import org.mule.examples.springboot.parity.ParityComparator;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * File-triggered parity: each fixture's {@code request.body} is dropped into the polled input folder
 * as {@code request.path}; the Salesforce calls (WireMock) must match {@code expectedOutbound} and the
 * folders must end up as described by {@code expected.body}.
 */
@SpringBootTest(properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
class ContactsToSfdcParityTest {

    static final String FIXTURES = "parity/import-contacts-into-salesforce";
    static final WireMockParitySupport DOWNSTREAM = new WireMockParitySupport().start();
    static final Path WORK = tempDir();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("parity.downstream.base-url", DOWNSTREAM::baseUrl);
        registry.add("file.input-path", () -> WORK.resolve("input").toString());
        registry.add("file.output-path", () -> WORK.resolve("output").toString());
    }

    @Autowired
    SalesforcePasswordSession session;

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load(FIXTURES).stream().map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    void runCase(ParityFixture fixture) throws IOException {
        session.invalidate();
        SalesforceParitySupport.stub(DOWNSTREAM, fixture);
        clean(WORK.resolve("input"));
        clean(WORK.resolve("output"));

        Path tmp = WORK.resolve(fixture.request().path() + ".tmp");
        Files.writeString(tmp, ParityFixtures.bodyText(fixture.request().body()), StandardCharsets.UTF_8);
        Files.move(tmp, WORK.resolve("input").resolve(fixture.request().path()));

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(100)).untilAsserted(() ->
                assertThat(DOWNSTREAM.verifyOutbound(fixture.expectedOutbound())).isEmpty());
        String folders = "{\"input\":" + json(list(WORK.resolve("input"))) + ",\"output\":" + json(list(WORK.resolve("output"))) + "}";
        assertThat(ParityComparator.compareBody(BodyFormat.JSON, ParityFixtures.bodyText(fixture.expected().body()), folders,
                fixture.expected().jsonCompareMode(), fixture.expected().ignorePaths())).isEmpty();
    }

    private static List<String> list(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private static String json(List<String> names) {
        return names.stream().map(n -> "\"" + n + "\"").reduce((a, b) -> a + "," + b).map(s -> "[" + s + "]").orElse("[]");
    }

    private static void clean(Path dir) throws IOException {
        Files.createDirectories(dir);
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.toList()) {
                Files.delete(p);
            }
        }
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("contacts-parity");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
