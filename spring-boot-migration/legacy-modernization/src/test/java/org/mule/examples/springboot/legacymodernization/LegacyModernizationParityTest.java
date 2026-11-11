package org.mule.examples.springboot.legacymodernization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.beans.factory.annotation.Value;

/**
 * Besides the SOAP response, each fixture's {@code expectedFiles} lists the CSV files the Mule flow's async scope
 * writes for that request ({@code nameRegex} + literal {@code content}); the Spring side must write the same files.
 */
class LegacyModernizationParityTest extends AbstractParityTest {

    private static final String DIR = "parity/legacy-modernization";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);

    @Value("${legacy.output.path}")
    private Path outputPath;

    @Override
    protected String fixtureDirectory() {
        return DIR;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        try {
            Files.createDirectories(outputPath);
            try (Stream<Path> files = Files.list(outputPath)) {
                for (Path f : files.toList()) {
                    Files.delete(f);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        JsonNode expectedFiles = rawFixture(fixture).path("expectedFiles");
        assertThat(expectedFiles.isArray()).as("%s declares expectedFiles", fixture.name()).isTrue();
        List<Path> written = awaitFiles(expectedFiles.size());
        assertThat(written).as("files written by %s", fixture.name()).hasSize(expectedFiles.size());
        for (int i = 0; i < written.size(); i++) {
            Path file = written.get(i);
            JsonNode expected = expectedFiles.get(i);
            assertThat(file.getFileName().toString()).matches(expected.get("nameRegex").asText());
            assertThat(read(file)).isEqualTo(expected.get("content").asText());
        }
    }

    private List<Path> awaitFiles(int expectedCount) {
        long deadline = System.nanoTime() + (expectedCount == 0 ? Duration.ofSeconds(1) : ASYNC_TIMEOUT).toNanos();
        List<Path> files = List.of();
        while (System.nanoTime() < deadline) {
            files = list();
            if (expectedCount > 0 && files.size() >= expectedCount) {
                break;
            }
            sleep();
        }
        sleep();
        return list();
    }

    private List<Path> list() {
        try (Stream<Path> files = Files.list(outputPath)) {
            return new ArrayList<>(files.sorted().toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode rawFixture(ParityFixture fixture) {
        try (InputStream in = LegacyModernizationParityTest.class.getClassLoader()
                .getResourceAsStream(DIR + "/" + fixture.name() + ".json")) {
            assertThat(in).as("fixture file for %s", fixture.name()).isNotNull();
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
