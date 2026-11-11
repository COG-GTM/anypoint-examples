package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Spring equivalent of {@code apikit:mapping-exception-strategy name="currency-apiKitGlobalExceptionMapping"} (currency.xml). */
@RestControllerAdvice
public class CurrencyApiExceptionMapping {

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<String> notFound() {
        return message(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<String> methodNotAllowed() {
        return message(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<String> notAcceptable() {
        return message(HttpStatus.NOT_ACCEPTABLE, "Not acceptable");
    }

    private static ResponseEntity<String> message(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body("{ \"message\": \"" + message + "\" }");
    }
}
