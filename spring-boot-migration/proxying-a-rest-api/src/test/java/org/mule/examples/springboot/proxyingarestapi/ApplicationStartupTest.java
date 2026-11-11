package org.mule.examples.springboot.proxyingarestapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Starts the app with its production defaults (no {@code parity} profile) so every placeholder must resolve. */
@SpringBootTest(classes = ProxyingARestApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationStartupTest {

    @Test
    void contextLoads() {
    }
}
