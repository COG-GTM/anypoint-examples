package org.mule.examples.springboot.parity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * One parity case: a request, the response the original Mule flow produces for it, and
 * optionally the downstream calls the flow makes. Loaded from JSON by {@link ParityFixtures}.
 *
 * <p>{@code body} fields accept either a JSON string (used verbatim) or any JSON value
 * (serialized compactly). {@code bodyFile} points at a file relative to the fixture file and
 * wins over {@code body}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ParityFixture(
        String name,
        String description,
        Source source,
        Request request,
        Expected expected,
        List<DownstreamStub> downstream,
        List<OutboundExpectation> expectedOutbound) {

    public ParityFixture {
        downstream = downstream == null ? List.of() : List.copyOf(downstream);
        expectedOutbound = expectedOutbound == null ? List.of() : List.copyOf(expectedOutbound);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Where the expected behaviour comes from. {@code evidence} is {@code documented} (derived from
     * the Mule flow XML / README), {@code recorded} (captured from a running Mule 3.8 runtime) or
     * {@code munit} (taken from an MUnit assertion).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(String muleApp, String flow, String evidence, String reference) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Request(String method, String path, Map<String, String> headers, JsonNode body, String bodyFile) {
        public Request {
            method = method == null ? "GET" : method.toUpperCase();
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }

    /**
     * Expected response. Header values are exact matches, {@code ~regex}, or {@code !absent}.
     * {@code ignorePaths} are JSONAssert paths (e.g. {@code id}, {@code **.timestamp}) excluded
     * from JSON comparison.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Expected(
            Integer status,
            Map<String, String> headers,
            BodyFormat bodyFormat,
            JsonNode body,
            String bodyFile,
            String jsonCompareMode,
            List<String> ignorePaths) {
        public Expected {
            headers = headers == null ? Map.of() : Map.copyOf(headers);
            bodyFormat = bodyFormat == null ? BodyFormat.IGNORE : bodyFormat;
            jsonCompareMode = jsonCompareMode == null ? "STRICT" : jsonCompareMode;
            ignorePaths = ignorePaths == null ? List.of() : List.copyOf(ignorePaths);
        }
    }

    /** A canned downstream response served by WireMock while the case runs. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DownstreamStub(
            String method,
            String urlPath,
            Map<String, String> queryParams,
            int status,
            Map<String, String> headers,
            JsonNode body,
            String bodyFile) {
        public DownstreamStub {
            method = method == null ? "ANY" : method.toUpperCase();
            queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
            status = status == 0 ? 200 : status;
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }

    /** A request the migrated app must send downstream (side-effect parity). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutboundExpectation(
            String method,
            String urlPath,
            Map<String, String> queryParams,
            Map<String, String> headers,
            BodyFormat bodyFormat,
            JsonNode body,
            String bodyFile,
            Integer count,
            String jsonCompareMode,
            List<String> ignorePaths) {
        public OutboundExpectation {
            method = method == null ? "ANY" : method.toUpperCase();
            queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
            headers = headers == null ? Map.of() : Map.copyOf(headers);
            bodyFormat = bodyFormat == null ? BodyFormat.IGNORE : bodyFormat;
            count = count == null ? 1 : count;
            jsonCompareMode = jsonCompareMode == null ? "STRICT" : jsonCompareMode;
            ignorePaths = ignorePaths == null ? List.of() : List.copyOf(ignorePaths);
        }
    }
}
