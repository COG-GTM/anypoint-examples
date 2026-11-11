package org.mule.examples.springboot.choiceexceptionstrategy;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** {@code choice-exception-strategy}: the first catch-exception-strategy whose {@code exception.causedBy(..)} matches wins. */
@ControllerAdvice
public class ChoiceExceptionStrategyAdvice {

    @ExceptionHandler(FlowException.class)
    public ResponseEntity<String> handle(FlowException e) {
        if (e.causedBy(IllegalArgumentException.class)) {
            return badRequest("Invalid input data: " + e.getPayload());
        }
        if (e.causedBy(NullPointerException.class)) {
            return badRequest("Missing input data: " + e.getPayload());
        }
        throw e;
    }

    private static ResponseEntity<String> badRequest(String body) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body(body);
    }
}
