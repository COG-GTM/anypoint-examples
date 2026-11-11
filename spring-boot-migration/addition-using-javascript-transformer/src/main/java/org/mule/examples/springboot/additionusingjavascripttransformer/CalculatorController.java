package org.mule.examples.springboot.additionusingjavascripttransformer;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code http:listener path="/" allowedMethods="POST"} + {@code byte-array-to-string-transformer}. */
@RestController
public class CalculatorController {

    private final CalculatorGateway gateway;

    public CalculatorController(CalculatorGateway gateway) {
        this.gateway = gateway;
    }

    @PostMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String calculate(@RequestBody(required = false) byte[] body) {
        return gateway.calculate(body == null ? "" : new String(body, StandardCharsets.UTF_8));
    }

    /** A script error propagates to Mule's default exception strategy: HTTP 500 with the error message. */
    @ExceptionHandler(MessagingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String scriptFailed(MessagingException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause.getMessage();
    }
}
