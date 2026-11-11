package org.mule.examples.springboot.authenticatingsalesforceusingoauth2;

import org.mule.examples.springboot.authenticatingsalesforceusingoauth2.salesforce.SalesforceToken;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mule.examples.springboot.parity.AbstractWireMockParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * OAuth2 authorization-code flow and contact query against the WireMock Salesforce API. A fixture's
 * {@code given.authorized} is either {@code false} (no token stored) or the token the connector already holds.
 */
class SalesforceOAuthParityTest extends AbstractWireMockParityTest {

    static final String FIXTURES = "parity/authenticating-salesforce-using-oauth2";
    static final Map<String, JsonNode> GIVEN = SalesforceParitySupport.given(FIXTURES);

    @Autowired
    SalesforceOAuth oauth;

    @Override
    protected String fixtureDirectory() {
        return FIXTURES;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        JsonNode authorized = GIVEN.get(fixture.name()).path("authorized");
        oauth.store(authorized.isObject()
                ? new SalesforceToken(authorized.path("access_token").asText(), DOWNSTREAM.baseUrl(),
                        authorized.path("refresh_token").asText(null))
                : null);
        SalesforceParitySupport.stub(DOWNSTREAM, fixture);
    }

    /** The connector refreshes an expired access token with the refresh token and retries the operation once. */
    @Test
    void expiredAccessTokenIsRefreshedAndQueryRetried() {
        DOWNSTREAM.reset();
        oauth.store(new SalesforceToken("00Dparity!EXPIRED", DOWNSTREAM.baseUrl(), "5Aepparity-refresh"));
        String query = "/services/data/v61.0/query";
        DOWNSTREAM.server().stubFor(get(urlPathEqualTo(query)).withHeader("Authorization", equalTo("Bearer 00Dparity!EXPIRED"))
                .willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")
                        .withBody("[{\"message\":\"Session expired or invalid\",\"errorCode\":\"INVALID_SESSION_ID\"}]")));
        DOWNSTREAM.server().stubFor(post(urlPathEqualTo("/services/oauth2/token"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"00Dparity!REFRESHED\",\"instance_url\":\"" + DOWNSTREAM.baseUrl() + "\"}")));
        DOWNSTREAM.server().stubFor(get(urlPathEqualTo(query)).withHeader("Authorization", equalTo("Bearer 00Dparity!REFRESHED"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"totalSize\":1,\"done\":true,\"records\":[{\"attributes\":{\"type\":\"Contact\"},"
                                + "\"Id\":\"0032000001INNfoAAH\",\"LastName\":\"Pickwick\",\"LastModifiedDate\":\"2014-08-25T13:21:00.000+0000\"}]}")));

        CapturedResponse response = http.send(springBaseUrl(), new ParityFixture.Request("GET", "/", Map.of(), null, null));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo("Salesforce query returned 1 contacts.");
        DOWNSTREAM.server().verify(postRequestedFor(urlPathEqualTo("/services/oauth2/token"))
                .withRequestBody(equalTo("grant_type=refresh_token&refresh_token=5Aepparity-refresh&client_id=parity-consumer-key&client_secret=parity-consumer-secret")));
        DOWNSTREAM.server().verify(2, getRequestedFor(urlPathEqualTo(query)));
        assertThat(oauth.current().refreshToken()).isEqualTo("5Aepparity-refresh");
    }
}
