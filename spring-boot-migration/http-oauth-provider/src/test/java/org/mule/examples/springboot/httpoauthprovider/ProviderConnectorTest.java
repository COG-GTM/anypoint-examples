package org.mule.examples.springboot.httpoauthprovider;

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

/** Mule's two ports: /authorize and /token on the provider port, /resources and /redirect on the listener port. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProviderConnectorTest {

    static final int PROVIDER_PORT = TestSocketUtils.findAvailableTcpPort();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("http.provider.port", () -> PROVIDER_PORT);
    }

    @LocalServerPort
    int port;

    private final ParityHttpClient http = new ParityHttpClient();

    private int status(int onPort, String path) {
        return http.send("http://localhost:" + onPort, new ParityFixture.Request("GET", path, Map.of(), null, null)).status();
    }

    @Test
    void endpointsStayOnTheirMulePorts() {
        assertThat(status(PROVIDER_PORT, ProviderGrantFlowTest.AUTHORIZE)).isEqualTo(200);
        assertThat(status(port, "/redirect?code=x")).isEqualTo(302);
        assertThat(status(port, "/resources")).isEqualTo(401);
        assertThat(status(port, ProviderGrantFlowTest.AUTHORIZE)).isEqualTo(404);
        assertThat(status(PROVIDER_PORT, "/redirect?code=x")).isEqualTo(404);
        assertThat(status(PROVIDER_PORT, "/resources")).isEqualTo(404);
    }
}
