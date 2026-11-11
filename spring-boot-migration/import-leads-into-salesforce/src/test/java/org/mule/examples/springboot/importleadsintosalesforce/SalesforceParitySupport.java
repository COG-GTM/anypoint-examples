package org.mule.examples.springboot.importleadsintosalesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Module-local parity helpers for the WireMock-backed Salesforce API.
 * <ul>
 *   <li>Salesforce returns the API host in the token response ({@code instance_url}); fixtures write it
 *       as {@value #BASE_URL_PLACEHOLDER} and it is replaced with the WireMock URL when stubbing.</li>
 *   <li>Fixtures may carry a {@code given} object (initial state: watermark, DB rows, clock...) that the
 *       shared {@link ParityFixture} model ignores; {@link #given} reads it.</li>
 * </ul>
 */
final class SalesforceParitySupport {

    static final String BASE_URL_PLACEHOLDER = "{{downstream.base-url}}";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SalesforceParitySupport() {
    }

    static void stub(WireMockParitySupport downstream, ParityFixture fixture) {
        downstream.reset();
        downstream.stub(fixture.downstream().stream().map(s -> new ParityFixture.DownstreamStub(
                s.method(), s.urlPath(), s.queryParams(), s.status(), s.headers(),
                substitute(s.body(), downstream.baseUrl()), null)).toList());
    }

    private static JsonNode substitute(JsonNode body, String baseUrl) {
        String text = ParityFixtures.bodyText(body);
        if (text == null || !text.contains(BASE_URL_PLACEHOLDER)) {
            return body;
        }
        return TextNode.valueOf(text.replace(BASE_URL_PLACEHOLDER, baseUrl));
    }

    /** {@code given} objects of every fixture in {@code dir}, keyed by fixture name. */
    static Map<String, JsonNode> given(String dir) {
        try {
            Map<String, JsonNode> given = new HashMap<>();
            for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:" + dir + "/*.json")) {
                JsonNode json = MAPPER.readTree(r.getInputStream());
                given.put(json.path("name").asText(), json.has("given") ? json.get("given") : MissingNode.getInstance());
            }
            return given;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> lines(String text, String prefix) {
        return text.lines().filter(l -> l.contains(prefix)).map(l -> l.substring(l.indexOf(prefix))).toList();
    }
}
