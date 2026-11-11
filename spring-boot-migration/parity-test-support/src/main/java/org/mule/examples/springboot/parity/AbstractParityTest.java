package org.mule.examples.springboot.parity;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Base class for HTTP parity tests. Every fixture under {@link #fixtureDirectory()} becomes a
 * dynamic test that:
 * <ol>
 *   <li>replays the request against the Spring Boot app (random port) and asserts the response
 *       matches the fixture (the Mule flow's documented or recorded behaviour);</li>
 *   <li>if a Mule runtime is available ({@code -Dparity.mule.base-url=http://localhost:8081}), replays
 *       the same request against it, asserts Mule matches the fixture and that Mule and Spring Boot
 *       responses are equivalent to each other (capture-and-compare);</li>
 *   <li>with {@code -Dparity.record=true}, writes the captured Mule response to
 *       {@code target/parity-recordings/} for promotion into fixtures.</li>
 * </ol>
 * Subclasses live in (or below) the package of their {@code @SpringBootApplication}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
public abstract class AbstractParityTest {

    public static final String MULE_BASE_URL_PROPERTY = "parity.mule.base-url";
    public static final String RECORD_PROPERTY = "parity.record";

    protected final ParityHttpClient http = new ParityHttpClient();

    @LocalServerPort
    protected int port;

    /** Classpath directory containing this module's fixture JSON files, e.g. {@code parity/hello-world}. */
    protected abstract String fixtureDirectory();

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load(fixtureDirectory()).stream()
                .map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    protected String springBaseUrl() {
        return "http://localhost:" + port;
    }

    /** Base URL of a running Mule 3.8 instance of the original app, if one is available. */
    protected Optional<String> muleBaseUrl() {
        return Optional.ofNullable(System.getProperty(MULE_BASE_URL_PROPERTY)).filter(s -> !s.isBlank());
    }

    protected Path recordingDirectory() {
        return Path.of("target", "parity-recordings");
    }

    protected boolean recording() {
        return Boolean.parseBoolean(System.getProperty(RECORD_PROPERTY, "false"));
    }

    /** Hook run before each case (e.g. seed a DB, register downstream stubs). */
    protected void beforeCase(ParityFixture fixture) {
    }

    /** Hook run after the Spring Boot response was asserted (e.g. verify side effects). */
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
    }

    protected void runCase(ParityFixture fixture) {
        beforeCase(fixture);
        CapturedResponse spring = http.send(springBaseUrl(), fixture.request());
        ParityAssertions.assertMatchesFixture(fixture, "spring-boot", spring);
        afterCase(fixture, spring);

        Optional<String> mule = muleBaseUrl();
        if (mule.isPresent()) {
            beforeCase(fixture);
            CapturedResponse muleResponse = http.send(mule.get(), fixture.request());
            if (recording()) {
                new ParityRecorder(recordingDirectory()).record(fixture, muleResponse);
            }
            ParityAssertions.assertMatchesFixture(fixture, "mule", muleResponse);
            ParityAssertions.assertEquivalent(fixture, muleResponse, spring);
        }
    }
}
