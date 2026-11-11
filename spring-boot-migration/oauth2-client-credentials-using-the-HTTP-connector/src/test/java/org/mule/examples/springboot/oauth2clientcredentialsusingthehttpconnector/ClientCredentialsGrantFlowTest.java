package org.mule.examples.springboot.oauth2clientcredentialsusingthehttpconnector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityHttpClient;
import org.mule.examples.springboot.parity.WireMockParitySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Client-credentials grant flow end to end (Mule 3.8 ClientCredentialsGrantType): token at startup
 * with Basic client authentication, Bearer on the API call, and on 401 a new token request followed
 * by exactly one resend with the new token. Runs on its own WireMock server and Spring context so
 * the token state cannot leak into the fixture-driven cases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.import=optional:classpath:/parity/parity-defaults.yml", "grant-flow-test=true"})
@ActiveProfiles("parity")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientCredentialsGrantFlowTest {

    static final WireMockParitySupport BOX = new WireMockParitySupport().start();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("parity.downstream.base-url", BOX::baseUrl);
    }

    @BeforeAll
    static void startupToken() {
        ClientCredentialsParityTest.stubTokenEndpoint(BOX.server(), "cc-token-1");
    }

    @AfterAll
    static void stop() {
        BOX.stop();
    }

    @LocalServerPort
    int port;

    @Autowired
    BoxTokenManager tokens;

    private final ParityHttpClient http = new ParityHttpClient();

    @Test
    @Order(1)
    void tokenIsFetchedAtStartupWithBasicClientAuthentication() {
        assertThat(tokens.accessToken()).isEqualTo("cc-token-1");
        assertThat(BOX.server().findAll(postRequestedFor(urlPathEqualTo("/api/oauth2/token")))).hasSize(1);
    }

    @Test
    @Order(2)
    void unauthorizedResponseRefreshesTokenAndResendsOnce() {
        WireMockServer box = BOX.server();
        box.resetRequests();
        box.stubFor(get(urlPathEqualTo("/api/oauth2/authorize/")).inScenario("refresh")
                .whenScenarioStateIs(Scenario.STARTED)
                .withHeader("Authorization", equalTo("Bearer cc-token-1"))
                .willReturn(aResponse().withStatus(401).withBody("expired"))
                .willSetStateTo("token-refreshed"));
        box.stubFor(get(urlPathEqualTo("/api/oauth2/authorize/")).inScenario("refresh")
                .whenScenarioStateIs("token-refreshed")
                .withHeader("Authorization", equalTo("Bearer cc-token-2"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain").withBody("after refresh")));
        ClientCredentialsParityTest.stubTokenEndpoint(box, "cc-token-2");

        CapturedResponse response = http.send("http://localhost:" + port,
                new ParityFixture.Request("GET", "/oauth", Map.of(), null, null));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo("after refresh");
        List<LoggedRequest> apiCalls = BOX.received("GET", "/api/oauth2/authorize/", Map.of());
        assertThat(apiCalls).extracting(r -> r.getHeader("Authorization"))
                .containsExactly("Bearer cc-token-1", "Bearer cc-token-2");
        assertThat(BOX.received("POST", "/api/oauth2/token", Map.of())).hasSize(1);
        assertThat(box.findAllUnmatchedRequests()).isEmpty();
        assertThat(tokens.accessToken()).isEqualTo("cc-token-2");
    }

    @Test
    @Order(3)
    void secondUnauthorizedResponseAfterRefreshFailsTheFlow() {
        WireMockServer box = BOX.server();
        box.resetAll();
        box.stubFor(get(urlPathEqualTo("/api/oauth2/authorize/"))
                .willReturn(aResponse().withStatus(403).withBody("forbidden")));
        ClientCredentialsParityTest.stubTokenEndpoint(box, "cc-token-3");

        CapturedResponse response = http.send("http://localhost:" + port,
                new ParityFixture.Request("GET", "/oauth", Map.of(), null, null));

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.bodyAsString()).startsWith("Response code 403 mapped as failure.");
        assertThat(BOX.received("GET", "/api/oauth2/authorize/", Map.of())).hasSize(2);
        assertThat(BOX.received("POST", "/api/oauth2/token", Map.of())).hasSize(1);
    }
}
