package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The browser journey of the Mule README (and HttpAuthorizationCodeWebIT) driven hop by hop, with
 * Box simulated by WireMock: /web → /authorization → Box authorize (user consents, Box redirects
 * to redirect_uri with code + the state it was given) → /redirectUrl → /web/loginDone. Also
 * covers Mule's refresh-on-401 for the default user. Separate WireMock and Spring context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.import=optional:classpath:/parity/parity-defaults.yml", "grant-flow-test=true"})
@ActiveProfiles("parity")
class AuthorizationCodeGrantFlowTest {

    static final WireMockParitySupport BOX = new WireMockParitySupport().start();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("parity.downstream.base-url", BOX::baseUrl);
    }

    @AfterAll
    static void stop() {
        BOX.stop();
    }

    @LocalServerPort
    int port;

    @Autowired
    BoxAuthorizationCodeClient oauth;

    private final ParityHttpClient http = new ParityHttpClient();

    @BeforeEach
    void reset() {
        BOX.reset();
        oauth.forgetTokens();
    }

    private CapturedResponse call(String pathAndQuery) {
        return http.send("http://localhost:" + port, new ParityFixture.Request("GET", pathAndQuery, Map.of(), null, null));
    }

    private static String location(CapturedResponse r) {
        return r.header("Location").orElseThrow();
    }

    private static String pathAndQuery(String url) {
        URI uri = URI.create(url);
        return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    private void tokenEndpointReturns(String grantType, String access, String refresh) {
        BOX.server().stubFor(post(urlPathEqualTo("/api/oauth2/token"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.containing("grant_type=" + grantType))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"" + access + "\",\"refresh_token\":\"" + refresh
                                + "\",\"token_type\":\"bearer\",\"expires_in\":3600}")));
    }

    @Test
    void fullAuthorizationCodeGrantFlow() {
        CapturedResponse web = call("/web");
        assertThat(web.status()).isEqualTo(302);
        String localAuthorization = location(web);
        assertThat(localAuthorization).startsWith("https://localhost:8082/authorization?");

        CapturedResponse authorize = call(pathAndQuery(localAuthorization));
        assertThat(authorize.status()).isEqualTo(302);
        Map<String, String> params = UriComponentsBuilder.fromUriString(location(authorize)).build().getQueryParams().toSingleValueMap();
        assertThat(params).containsEntry("response_type", "code").containsEntry("client_id", "parity-box-id")
                .containsEntry("box_device_id", "1").containsEntry("box_device_name", "device1");
        String state = URLDecoder.decode(params.get("state"), StandardCharsets.UTF_8);
        String redirectUri = URLDecoder.decode(params.get("redirect_uri"), StandardCharsets.UTF_8);
        assertThat(state).isEqualTo(":onCompleteRedirectTo=http://localhost:8081/web/loginDone");
        assertThat(redirectUri).isEqualTo("https://localhost:8082/redirectUrl");

        tokenEndpointReturns("authorization_code", "flow-access", "flow-refresh");
        String boxCallback = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", "flow-code").queryParam("state", "{state}").encode().buildAndExpand(state).toUriString();
        CapturedResponse callback = call(pathAndQuery(boxCallback));
        assertThat(callback.status()).isEqualTo(302);
        assertThat(location(callback)).isEqualTo("http://localhost:8081/web/loginDone?authorizationStatus=0");
        String tokenRequest = BOX.received("POST", "/api/oauth2/token", Map.of()).get(0).getBodyAsString();
        assertThat(tokenRequest).contains("grant_type=authorization_code", "code=flow-code", "client_id=parity-box-id",
                "client_secret=parity-box-secret", "redirect_uri=https%3A%2F%2Flocalhost%3A8082%2FredirectUrl");

        BOX.server().stubFor(get(urlPathEqualTo("/2.0/search")).withQueryParam("query", equalTo("mule"))
                .withHeader("Authorization", equalTo("Bearer flow-access"))
                .willReturn(aResponse().withBody("{\"entries\":[]}")));
        CapturedResponse done = call(pathAndQuery(location(callback)));
        assertThat(done.status()).isEqualTo(200);
        assertThat(done.bodyAsString()).isEqualTo(
                "The user successfully authorized access to the resource. Searching for files containing \"mule\": {\"entries\":[]}");
        assertThat(BOX.server().findAllUnmatchedRequests()).isEmpty();
    }

    @Test
    void unauthorizedApiResponseRefreshesTokenAndResendsOnce() {
        tokenEndpointReturns("authorization_code", "old-access", "old-refresh");
        assertThat(call("/redirectUrl?code=c1").status()).isEqualTo(200);

        WireMockServer box = BOX.server();
        box.stubFor(get(urlPathEqualTo("/2.0/search")).inScenario("refresh").whenScenarioStateIs(Scenario.STARTED)
                .withHeader("Authorization", equalTo("Bearer old-access"))
                .willReturn(aResponse().withStatus(401)).willSetStateTo("refreshed"));
        box.stubFor(get(urlPathEqualTo("/2.0/search")).inScenario("refresh").whenScenarioStateIs("refreshed")
                .withHeader("Authorization", equalTo("Bearer new-access"))
                .willReturn(aResponse().withBody("found")));
        tokenEndpointReturns("refresh_token", "new-access", "new-refresh");

        CapturedResponse done = call("/web/loginDone");

        assertThat(done.status()).isEqualTo(200);
        assertThat(done.bodyAsString()).endsWith("\"mule\": found");
        String refreshRequest = BOX.received("POST", "/api/oauth2/token", Map.of()).get(1).getBodyAsString();
        assertThat(refreshRequest).contains("grant_type=refresh_token", "refresh_token=old-refresh", "client_id=parity-box-id",
                "client_secret=parity-box-secret", "redirect_uri=https%3A%2F%2Flocalhost%3A8082%2FredirectUrl");
        assertThat(BOX.received("GET", "/2.0/search", Map.of())).extracting(r -> r.getHeader("Authorization"))
                .containsExactly("Bearer old-access", "Bearer new-access");
    }
}
