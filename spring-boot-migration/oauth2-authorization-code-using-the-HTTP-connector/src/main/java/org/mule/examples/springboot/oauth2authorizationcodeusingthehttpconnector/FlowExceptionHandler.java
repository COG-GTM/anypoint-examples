package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mule default exception strategy + HTTP listener failure response: 500 with the exception message. */
@RestControllerAdvice
class FlowExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FlowExceptionHandler.class);

    @ExceptionHandler(MuleFlowException.class)
    ResponseEntity<String> flowFailure(MuleFlowException e) {
        log.error("{}", e.toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(e.getMessage());
    }
}
