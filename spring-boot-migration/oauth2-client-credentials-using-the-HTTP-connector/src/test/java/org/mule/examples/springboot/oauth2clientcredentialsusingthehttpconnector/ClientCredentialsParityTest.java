package org.mule.examples.springboot.oauth2clientcredentialsusingthehttpconnector;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeAll;
import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

class ClientCredentialsParityTest extends AbstractWireMockParityTest {

    /** Base64 of "parity-client:parity-secret", as Mule's ClientCredentialsTokenRequestHandler sends it. */
    static final String BASIC = "Basic cGFyaXR5LWNsaWVudDpwYXJpdHktc2VjcmV0";

    /**
     * Mule fetches the client-credentials token while the app starts, so the token endpoint has to be
     * stubbed before the Spring context exists. The stub only matches the Mule-shaped token request
     * (Basic client auth, grant_type=client_credentials form); anything else fails startup.
     */
    @BeforeAll
    static void stubStartupTokenEndpoint() {
        stubTokenEndpoint(DOWNSTREAM.server(), "cc-token-1");
    }

    static void stubTokenEndpoint(com.github.tomakehurst.wiremock.WireMockServer server, String token) {
        server.stubFor(post(urlPathEqualTo("/api/oauth2/token"))
                .withHeader("Authorization", equalTo(BASIC))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(WireMock.equalTo("grant_type=client_credentials"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"" + token + "\",\"token_type\":\"bearer\",\"expires_in\":3600}")));
    }

    @Override
    protected String fixtureDirectory() {
        return "parity/oauth2-client-credentials-using-the-HTTP-connector";
    }
}
