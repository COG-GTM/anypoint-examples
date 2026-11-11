package org.mule.examples.springboot.loginformusingthehttpconnector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;

/** Side effect of doLogin: {@code <logger level="INFO" message="User #[payload.username] is trying to login"/>}. */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginLoggerSideEffectTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void requesterLoginLogsTheLoopbackLogin(CapturedOutput output) {
        assertThat(rest.getForEntity("/requesterLogin", String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(output.getOut()).contains("INFO").contains("User mule is trying to login");
    }
}
