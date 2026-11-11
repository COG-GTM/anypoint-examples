package org.mule.examples.springboot.sapdataretrieval;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** {@code apikit:mapping-exception-strategy name="sap-api-apiKitGlobalExceptionMapping"}. */
@RestControllerAdvice
public class ApiKitExceptionMapping {

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<String> notFound() {
        return json(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<String> methodNotAllowed() {
        return json(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<String> unsupportedMediaType() {
        return json(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<String> badRequest() {
        return json(HttpStatus.BAD_REQUEST, "Bad request");
    }

    private static ResponseEntity<String> json(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{ \"message\": \"" + message + "\" }");
    }
}
