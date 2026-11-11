package org.mule.examples.springboot.parity;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * WireMock helpers for downstream systems (Salesforce, NetSuite, SOAP backends, proxied APIs...):
 * registers a fixture's {@code downstream} stubs and verifies its {@code expectedOutbound} requests.
 */
public class WireMockParitySupport {

    private final WireMockServer server;

    public WireMockParitySupport() {
        this(new WireMockServer(wireMockConfig().dynamicPort()));
    }

    public WireMockParitySupport(WireMockServer server) {
        this.server = server;
    }

    public WireMockParitySupport start() {
        if (!server.isRunning()) {
            server.start();
        }
        return this;
    }

    public void stop() {
        server.stop();
    }

    public WireMockServer server() {
        return server;
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    public void reset() {
        server.resetAll();
    }

    public void stub(List<ParityFixture.DownstreamStub> stubs) {
        for (ParityFixture.DownstreamStub s : stubs) {
            MappingBuilder mapping = WireMock.request(s.method(), WireMock.urlPathEqualTo(s.urlPath()));
            s.queryParams().forEach((k, v) -> mapping.withQueryParam(k, WireMock.equalTo(v)));
            ResponseDefinitionBuilder response = WireMock.aResponse().withStatus(s.status());
            s.headers().forEach(response::withHeader);
            String body = ParityFixtures.bodyText(s.body());
            if (body != null) {
                response.withBody(body);
            }
            server.stubFor(mapping.willReturn(response));
        }
    }

    /** All requests WireMock received for the given method (or {@code ANY}) and path. */
    public List<LoggedRequest> received(String method, String urlPath, Map<String, String> queryParams) {
        List<LoggedRequest> matches = new ArrayList<>();
        for (var event : server.getAllServeEvents()) {
            LoggedRequest r = event.getRequest();
            String path = r.getUrl().split("\\?", 2)[0];
            boolean methodOk = "ANY".equals(method) || r.getMethod().getName().equalsIgnoreCase(method);
            boolean queryOk = queryParams.entrySet().stream().allMatch(q -> {
                var p = r.queryParameter(q.getKey());
                return p.isPresent() && p.values().contains(q.getValue());
            });
            if (methodOk && path.equals(urlPath) && queryOk) {
                matches.add(r);
            }
        }
        // serve events are newest-first
        java.util.Collections.reverse(matches);
        return matches;
    }

    /** Verifies the outbound side effects declared in a fixture; returns mismatches. */
    public List<String> verifyOutbound(List<ParityFixture.OutboundExpectation> expectations) {
        List<String> mismatches = new ArrayList<>();
        for (ParityFixture.OutboundExpectation o : expectations) {
            String label = "outbound " + o.method() + " " + o.urlPath();
            List<LoggedRequest> got = received(o.method(), o.urlPath(), o.queryParams());
            if (got.size() != o.count()) {
                mismatches.add(label + ": expected " + o.count() + " request(s) but got " + got.size());
                continue;
            }
            for (LoggedRequest r : got) {
                o.headers().forEach((name, want) -> {
                    String value = r.getHeader(name);
                    if (ParityComparator.ABSENT.equals(want)) {
                        if (value != null) {
                            mismatches.add(label + " header " + name + ": expected absent but was '" + value + "'");
                        }
                    } else if (value == null || !ParityComparator.textMatches(want, value)) {
                        mismatches.add(label + " header " + name + ": expected '" + want + "' but was '" + value + "'");
                    }
                });
                ParityComparator.compareBody(o.bodyFormat(), ParityFixtures.bodyText(o.body()), r.getBodyAsString(),
                        o.jsonCompareMode(), o.ignorePaths()).forEach(m -> mismatches.add(label + " " + m));
            }
        }
        return mismatches;
    }
}
