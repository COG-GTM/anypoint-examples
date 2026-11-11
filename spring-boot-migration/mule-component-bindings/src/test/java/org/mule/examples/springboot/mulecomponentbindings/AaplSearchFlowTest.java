package org.mule.examples.springboot.mulecomponentbindings;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** mule-component-bindingsFlow1: GET / runs the fixed twitter:search query="AAPL" since 2014-10-26 until 2014-11-04. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AaplSearchFlowTest {

    static final WireMockServer TWITTER = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        TWITTER.start();
        registry.add("twitter.base-url", TWITTER::baseUrl);
        registry.add("twitter.bearer-token", () -> "test-token");
    }

    @AfterAll
    static void stop() {
        TWITTER.stop();
    }

    @Autowired
    TestRestTemplate http;

    @Test
    void searchesAaplWithFixedWindow() {
        TWITTER.stubFor(get(urlPathEqualTo("/1.1/search/tweets.json"))
                .willReturn(okJson("{\"statuses\":[{\"id\":42,\"text\":\"AAPL up\"}]}")));
        String body = http.getForObject("/", String.class);
        assertThat(body).isEqualTo("[{\"id\":\"42\",\"text\":\"AAPL up\",\"sentiment\":null}]");
        TWITTER.verify(1, getRequestedFor(urlPathEqualTo("/1.1/search/tweets.json"))
                .withQueryParam("q", equalTo("AAPL")).withQueryParam("since", equalTo("2014-10-26"))
                .withQueryParam("until", equalTo("2014-11-04"))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }
}
