package org.mule.examples.springboot.parity;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The harness must accept equivalent responses and reject divergent ones. */
class ParityComparatorTest {

    private static CapturedResponse response(int status, String contentType, String body) {
        return new CapturedResponse(status, Map.of("Content-Type", List.of(contentType)), body.getBytes(StandardCharsets.UTF_8));
    }

    private static ParityFixture.Expected expected(int status, BodyFormat format, String body, Map<String, String> headers) {
        return new ParityFixture.Expected(status, headers, format, TextNode.valueOf(body), null, null, null);
    }

    @Test
    void jsonKeyOrderAndWhitespaceAreIgnored() {
        var exp = expected(200, BodyFormat.JSON, "{\"a\":1,\"b\":[1,2]}", Map.of());
        assertThat(ParityComparator.compare(exp, response(200, "application/json", "{ \"b\": [1, 2], \"a\": 1 }"))).isEmpty();
    }

    @Test
    void jsonValueDifferenceIsReported() {
        var exp = expected(200, BodyFormat.JSON, "{\"a\":1}", Map.of());
        assertThat(ParityComparator.compare(exp, response(200, "application/json", "{\"a\":2}")))
                .singleElement().asString().contains("json body").contains("a");
    }

    @Test
    void jsonExtraFieldIsReportedInStrictMode() {
        var exp = expected(200, BodyFormat.JSON, "{\"a\":1}", Map.of());
        assertThat(ParityComparator.compare(exp, response(200, "application/json", "{\"a\":1,\"b\":2}"))).hasSize(1);
    }

    @Test
    void jsonIgnorePathsSkipVolatileFields() {
        var exp = new ParityFixture.Expected(200, Map.of(), BodyFormat.JSON, TextNode.valueOf("{\"id\":\"x\",\"a\":1}"),
                null, null, List.of("id"));
        assertThat(ParityComparator.compare(exp, response(200, "application/json", "{\"id\":\"y\",\"a\":1}"))).isEmpty();
    }

    @Test
    void xmlWhitespaceAndNamespacePrefixesAreSimilar() {
        var exp = expected(200, BodyFormat.XML,
                "<a:greeting xmlns:a=\"urn:sample\">\n  <a:text>Hello</a:text>\n</a:greeting>", Map.of());
        String actual = "<b:greeting xmlns:b=\"urn:sample\"><b:text>Hello</b:text></b:greeting>";
        assertThat(ParityComparator.compare(exp, response(200, "text/xml", actual))).isEmpty();
    }

    @Test
    void xmlTextDifferenceIsReported() {
        var exp = expected(200, BodyFormat.XML, "<greeting><text>Hello</text></greeting>", Map.of());
        assertThat(ParityComparator.compare(exp, response(200, "text/xml", "<greeting><text>Bye</text></greeting>")))
                .isNotEmpty();
    }

    @Test
    void statusAndHeaderMismatchesAreReported() {
        var exp = expected(201, BodyFormat.IGNORE, "", Map.of("Content-Type", "~application/json.*", "X-Absent", "!absent"));
        List<String> mismatches = ParityComparator.compare(exp, response(200, "text/plain", "x"));
        assertThat(mismatches).hasSize(2);
        assertThat(mismatches.get(0)).startsWith("status");
        assertThat(mismatches.get(1)).startsWith("header Content-Type");
    }

    @Test
    void textSupportsRegex() {
        var exp = expected(200, BodyFormat.TEXT, "~Order [0-9]+ accepted", Map.of());
        assertThat(ParityComparator.compare(exp, response(200, "text/plain", "Order 42 accepted"))).isEmpty();
        assertThat(ParityComparator.compare(exp, response(200, "text/plain", "Order x accepted"))).hasSize(1);
    }

    @Test
    void muleVsSpringDivergenceFailsTheCase() {
        var fixture = new ParityFixture("divergent", null, null,
                new ParityFixture.Request("GET", "/x", null, null, null),
                expected(200, BodyFormat.JSON, "{\"a\":1}", Map.of("Content-Type", "application/json")), null, null);
        CapturedResponse mule = response(200, "application/json", "{\"a\":1}");
        CapturedResponse spring = response(200, "application/json;charset=UTF-8", "{\"a\":1}");
        assertThatThrownBy(() -> ParityAssertions.assertEquivalent(fixture, mule, spring))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("header Content-Type");
    }

    @Test
    void capturedMuleTextIsComparedLiterallyNotAsRegex() {
        var fixture = new ParityFixture("tilde", null, null,
                new ParityFixture.Request("GET", "/x", null, null, null),
                expected(200, BodyFormat.TEXT, "~.*", Map.of()), null, null);
        CapturedResponse mule = response(200, "text/plain", "~.*");
        CapturedResponse spring = response(200, "text/plain", "anything");
        assertThatThrownBy(() -> ParityAssertions.assertEquivalent(fixture, mule, spring))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("body: mule");
    }

    @Test
    void unstubbedDownstreamCallIsReported() throws Exception {
        WireMockParitySupport wm = new WireMockParitySupport().start();
        try {
            var http = java.net.http.HttpClient.newHttpClient();
            http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(wm.baseUrl() + "/surprise")).build(),
                    java.net.http.HttpResponse.BodyHandlers.discarding());
            assertThat(wm.verifyOutbound(List.of()))
                    .singleElement().asString().contains("unexpected outbound GET /surprise");
        } finally {
            wm.stop();
        }
    }
}
