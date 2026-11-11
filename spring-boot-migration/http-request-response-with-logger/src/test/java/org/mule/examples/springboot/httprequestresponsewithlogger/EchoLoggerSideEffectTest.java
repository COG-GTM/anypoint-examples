package org.mule.examples.springboot.httprequestresponsewithlogger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;

/** Side effect of EchoFlow: {@code <logger message="About to echo #[message.payload]" level="INFO"/>}. */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EchoLoggerSideEffectTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void logsEchoedPathAtInfo(CapturedOutput output) {
        assertThat(rest.getForObject("/moon", String.class)).isEqualTo("/moon");
        assertThat(output.getOut()).contains("INFO").contains("About to echo /moon");
    }
}
