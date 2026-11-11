package org.mule.examples.springboot.importcontactsintomsdynamics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.ParityComparator;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * File-driven parity: each fixture's {@code request.body} is dropped into the polled input directory as
 * {@code request.path}; WireMock plays Dynamics. Asserts the Dynamics create calls (in CSV row order) and that the file
 * was moved to the output directory. This flow has no HTTP listener, so it can't use {@code AbstractParityTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
class ImportContactsIntoMsDynamicsParityTest {

    static final WireMockParitySupport DYNAMICS = new WireMockParitySupport().start();
    static final Path INPUT = tempDir("input");
    static final Path OUTPUT = tempDir("output");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("parity.downstream.base-url", DYNAMICS::baseUrl);
        registry.add("contacts.input-dir", INPUT::toString);
        registry.add("contacts.output-dir", OUTPUT::toString);
    }

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load("parity/import-contacts-into-ms-dynamics").stream()
                .map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    void runCase(ParityFixture fixture) throws IOException, InterruptedException {
        DYNAMICS.reset();
        DYNAMICS.stub(fixture.downstream());
        String fileName = fixture.request().path();
        int expectedCalls = fixture.expectedOutbound().stream().mapToInt(ParityFixture.OutboundExpectation::count).sum();

        Path tmp = INPUT.resolveSibling(fileName + ".tmp");
        Files.writeString(tmp, ParityFixtures.bodyText(fixture.request().body()), StandardCharsets.UTF_8);
        Files.move(tmp, INPUT.resolve(fileName));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(OUTPUT.resolve(fileName)).as("file moved to output directory").exists();
            assertThat(INPUT.resolve(fileName)).as("file removed from input directory").doesNotExist();
            assertThat(DYNAMICS.server().getAllServeEvents()).hasSizeGreaterThanOrEqualTo(expectedCalls);
        });
        Thread.sleep(500);

        List<String> mismatches = new ArrayList<>(DYNAMICS.verifyOutbound(List.of()));
        List<LoggedRequest> calls = DYNAMICS.server().getAllServeEvents().stream()
                .map(e -> e.getRequest())
                .sorted(Comparator.comparing(LoggedRequest::getLoggedDate))
                .toList();
        if (calls.size() != expectedCalls) {
            mismatches.add("expected " + expectedCalls + " Dynamics call(s) but got " + calls.size());
        } else {
            int i = 0;
            for (ParityFixture.OutboundExpectation o : fixture.expectedOutbound()) {
                for (int n = 0; n < o.count(); n++, i++) {
                    mismatches.addAll(compare(i, o, calls.get(i)));
                }
            }
        }
        assertThat(mismatches).as(fixture.name() + " (" + fixture.source().reference() + ")").isEmpty();
    }

    static List<String> compare(int index, ParityFixture.OutboundExpectation o, LoggedRequest r) {
        String label = "call #" + (index + 1) + " " + o.method() + " " + o.urlPath();
        List<String> mismatches = new ArrayList<>();
        if (!o.method().equals(r.getMethod().getName()) || !o.urlPath().equals(r.getUrl().replaceAll("\\?.*", ""))) {
            mismatches.add(label + ": got " + r.getMethod().getName() + " " + r.getUrl());
        }
        for (Map.Entry<String, String> h : o.headers().entrySet()) {
            String value = r.getHeader(h.getKey());
            if (value == null || !headerMatches(h.getValue(), value)) {
                mismatches.add(label + " header " + h.getKey() + ": expected '" + h.getValue() + "' but was '" + value + "'");
            }
        }
        ParityComparator.compareBody(o.bodyFormat(), ParityFixtures.bodyText(o.body()), r.getBodyAsString(),
                o.jsonCompareMode(), o.ignorePaths()).forEach(m -> mismatches.add(label + " " + m));
        return mismatches;
    }

    /** Same header syntax as fixtures elsewhere: exact value, or a regex prefixed with {@code ~}. */
    static boolean headerMatches(String expected, String actual) {
        return expected.startsWith("~") ? actual.matches(expected.substring(1)) : expected.equals(actual);
    }

    private static Path tempDir(String name) {
        try {
            return Files.createDirectories(Files.createTempDirectory("ms-dynamics-parity").resolve(name));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
