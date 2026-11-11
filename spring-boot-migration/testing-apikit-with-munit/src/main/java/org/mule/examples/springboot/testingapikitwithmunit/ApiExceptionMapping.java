package org.mule.examples.springboot.testingapikitwithmunit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** {@code apikit:mapping-exception-strategy name="api-apiKitGlobalExceptionMapping"} from api.xml. */
@RestControllerAdvice
public class ApiExceptionMapping {

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<String> notFound() {
        return error(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<String> methodNotAllowed() {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<String> unsupportedMediaType() {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<String> notAcceptable() {
        return error(HttpStatus.NOT_ACCEPTABLE, "Not acceptable");
    }

    private static ResponseEntity<String> error(HttpStatus status, String message) {
        // Content-Type is preset so the body is written even when the client's Accept header excludes JSON.
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{ \"message\": \"" + message + "\" }");
    }
}
