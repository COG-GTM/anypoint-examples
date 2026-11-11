package org.mule.examples.springboot.httpoauthprovider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityComparator;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityHttpClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The README's OAuth dance end to end (authorize → login → code → /redirect → GET /token → /resources).
 * Codes and tokens are only known at run time, so these fixtures under parity/http-oauth-provider-flow
 * are templates ({{code}}, {{access_token}}) filled in here and compared with ParityComparator.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
class ProviderGrantFlowTest {

    static final String AUTHORIZE = "/authorize?response_type=code&client_id=myclientid&scope=READ_RESOURCE&redirect_uri=http://localhost:8082/redirect";
    static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @LocalServerPort
    int port;

    private final ParityHttpClient http = new ParityHttpClient();

    private CapturedResponse send(String method, String path, Map<String, String> headers, String body) {
        return http.send("http://localhost:" + port,
                new ParityFixture.Request(method, path, headers, body == null ? null : JSON.getNodeFactory().textNode(body), null));
    }

    private static ParityFixture template(String name, Map<String, String> values) throws IOException {
        String json = new ClassPathResource("parity/http-oauth-provider-flow/" + name + ".json").getContentAsString(StandardCharsets.UTF_8);
        for (Map.Entry<String, String> value : values.entrySet()) {
            json = json.replace("{{" + value.getKey() + "}}", value.getValue());
        }
        return JSON.readValue(json, ParityFixture.class);
    }

    private CapturedResponse run(ParityFixture fixture) {
        CapturedResponse response = http.send("http://localhost:" + port, fixture.request());
        assertThat(ParityComparator.compare(fixture.expected(), response)).as(fixture.name()).isEmpty();
        return response;
    }

    private String loginAndGetCode() {
        CapturedResponse login = send("POST", AUTHORIZE, Map.of("Content-Type", "application/x-www-form-urlencoded"),
                "username=mule&password=mule");
        assertThat(login.status()).isEqualTo(302);
        String location = login.header("Location").orElseThrow();
        assertThat(location).startsWith("http://localhost:8082/redirect?code=");
        return UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");
    }

    @Test
    void readmeOAuthDance() throws IOException {
        assertThat(send("GET", AUTHORIZE, Map.of(), null).status()).isEqualTo(200);
        String code = loginAndGetCode();

        CapturedResponse redirect = send("GET", "/redirect?code=" + code, Map.of(), null);
        String tokenUrl = redirect.header("Location").orElseThrow();
        assertThat(tokenUrl).startsWith("http://localhost:8081/token?grant_type=authorization_code&&");
        ParityFixture exchange = template("token-exchange", Map.of("code", code));
        assertThat(exchange.request().path()).isEqualTo(tokenUrl.substring("http://localhost:8081".length()));

        CapturedResponse token = run(exchange);
        String accessToken = JSON.readTree(token.bodyAsString()).get("access_token").asText();
        assertThat(accessToken).matches("[A-Za-z0-9_\\-]{20,}");
        run(template("protected-resource", Map.of("access_token", accessToken)));

        CapturedResponse reused = http.send("http://localhost:" + port, exchange.request());
        assertThat(reused.status()).as("authorization codes are single use").isEqualTo(400);
    }

    @Test
    void formPostTokenRequestHasSameShape() throws IOException {
        CapturedResponse token = run(template("token-exchange-post", Map.of("code", loginAndGetCode())));
        JsonNode body = JSON.readTree(token.bodyAsString());
        assertThat(body.has("refresh_token")).as("only AUTHORIZATION_CODE is enabled, no refresh tokens").isFalse();
    }

    @Test
    void wrongPasswordShowsLoginAgainWithoutRedirect() {
        CapturedResponse login = send("POST", AUTHORIZE, Map.of("Content-Type", "application/x-www-form-urlencoded"),
                "username=mule&password=wrong");
        assertThat(login.status()).isEqualTo(401);
        assertThat(login.header("Location")).isEmpty();
        assertThat(login.bodyAsString()).contains("id=\"username\"");
    }

    @Test
    void redirectUriOutsideRegisteredPatternIsRejected() {
        CapturedResponse login = send("POST", AUTHORIZE.replace("http://localhost:8082/redirect", "http://evil.example/cb"),
                Map.of("Content-Type", "application/x-www-form-urlencoded"), "username=mule&password=mule");
        assertThat(login.status()).isEqualTo(400);
        assertThat(login.header("Location")).isEmpty();
    }
}
