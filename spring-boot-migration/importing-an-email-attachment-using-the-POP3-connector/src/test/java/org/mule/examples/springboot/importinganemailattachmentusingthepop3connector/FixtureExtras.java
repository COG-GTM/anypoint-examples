package org.mule.examples.springboot.importinganemailattachmentusingthepop3connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityComparator;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the module-specific parts of a parity fixture that the shared {@code ParityFixture} model does not cover
 * (side effects such as uploaded files, sent emails or stored documents), and compares them with
 * {@link ParityComparator} so JSON/XML/text equivalence works exactly like for HTTP responses.
 */
final class FixtureExtras {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FixtureExtras() {
    }

    /** The raw JSON of the fixture called {@code name} under {@code classpath:<dir>/}. */
    static JsonNode raw(String dir, String name) {
        try {
            for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:" + dir + "/*.json")) {
                JsonNode node = MAPPER.readTree(r.getInputStream());
                if (name.equals(node.path("name").asText())) {
                    return node;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new IllegalStateException("No fixture named " + name + " under " + dir);
    }

    /** Parses a side-effect block written in the same shape as a fixture's {@code expected} section. */
    static ParityFixture.Expected expected(JsonNode block) {
        try {
            return MAPPER.treeToValue(block, ParityFixture.Expected.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Asserts a captured side effect (headers + body) matches an {@code expected}-shaped block. */
    static void assertSideEffect(ParityFixture fixture, String what, JsonNode block,
                                 Map<String, List<String>> headers, byte[] body) {
        List<String> mismatches = ParityComparator.compare(expected(block), new CapturedResponse(0, headers, body));
        assertThat(mismatches).as("Parity mismatch [%s] %s; actual body=%s", fixture.name(), what,
                new String(body, java.nio.charset.StandardCharsets.UTF_8)).isEmpty();
    }
}
