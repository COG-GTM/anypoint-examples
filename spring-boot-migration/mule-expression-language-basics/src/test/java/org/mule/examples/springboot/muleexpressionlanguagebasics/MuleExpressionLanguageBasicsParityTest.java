package org.mule.examples.springboot.muleexpressionlanguagebasics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP parity for the six greeting flows, plus the file:outbound-endpoint side effect: each fixture's
 * {@code expectedFile} holds the content the Mule app writes (compared trimmed, like MuleExpressionLanguageBasicsIT);
 * a missing/null {@code expectedFile} means the flow writes no file.
 */
class MuleExpressionLanguageBasicsParityTest extends AbstractWireMockParityTest {

    private static final String FIXTURES = "parity/mule-expression-language-basics";
    private static final Path OUTPUT_DIR = createOutputDir();

    @DynamicPropertySource
    static void outputDir(DynamicPropertyRegistry registry) {
        registry.add("mel.output-dir", OUTPUT_DIR::toString);
    }

    @Override
    protected String fixtureDirectory() {
        return FIXTURES;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        super.beforeCase(fixture);
        outputFiles().forEach(f -> f.toFile().delete());
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        super.afterCase(fixture, spring);
        JsonNode expected = rawFixture(fixture.name()).get("expectedFile");
        List<Path> files = outputFiles();
        if (expected == null || expected.isNull()) {
            assertThat(files).as("[%s] files written", fixture.name()).isEmpty();
            return;
        }
        assertThat(files).as("[%s] files written", fixture.name()).hasSize(1);
        try {
            assertThat(Files.readString(files.get(0), StandardCharsets.UTF_8).trim())
                    .as("[%s] file content", fixture.name()).isEqualTo(expected.asText().trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> outputFiles() {
        try (Stream<Path> files = Files.list(OUTPUT_DIR)) {
            return files.toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonNode rawFixture(String name) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath:" + FIXTURES + "/*.json")) {
                JsonNode node = mapper.readTree(r.getInputStream());
                if (name.equals(node.path("name").asText())) {
                    return node;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new IllegalStateException("No fixture named " + name);
    }

    private static Path createOutputDir() {
        try {
            return Files.createTempDirectory("mel-output");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
