package org.mule.examples.springboot.soapwebservicesecurity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Starts the app with its production defaults (no {@code parity} profile); only the ports are randomised. */
@SpringBootTest(classes = SoapWebserviceSecurityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "security.client.port=0")
class ApplicationStartupTest {

    @Test
    void contextLoads() {
    }
}
