package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import org.junit.jupiter.api.Test;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityHttpClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.TestSocketUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Mule's two listeners: /web* only on the main port, the OAuth callbacks only on the callback port. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"box.id=connector-id", "box.secret=connector-secret"})
class CallbackConnectorTest {

    static final int CALLBACK_PORT = TestSocketUtils.findAvailableTcpPort();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("oauth.local-authorization-url", () -> "http://localhost:" + CALLBACK_PORT + "/authorization");
        registry.add("oauth.redirection-url", () -> "http://localhost:" + CALLBACK_PORT + "/redirectUrl");
    }

    @LocalServerPort
    int port;

    private final ParityHttpClient http = new ParityHttpClient();

    private int status(int onPort, String path) {
        return http.send("http://localhost:" + onPort, new ParityFixture.Request("GET", path, Map.of(), null, null)).status();
    }

    @Test
    void listenersStayOnTheirOwnPorts() {
        assertThat(status(port, "/web")).isEqualTo(302);
        assertThat(status(CALLBACK_PORT, "/authorization")).isEqualTo(302);
        assertThat(status(CALLBACK_PORT, "/redirectUrl")).isEqualTo(400);
        assertThat(status(port, "/authorization")).isEqualTo(404);
        assertThat(status(port, "/redirectUrl")).isEqualTo(404);
        assertThat(status(CALLBACK_PORT, "/web")).isEqualTo(404);
    }
}
