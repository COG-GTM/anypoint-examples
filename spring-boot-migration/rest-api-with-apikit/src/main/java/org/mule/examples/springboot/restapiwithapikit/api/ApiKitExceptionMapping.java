package org.mule.examples.springboot.restapiwithapikit.api;

import org.mule.examples.springboot.restapiwithapikit.api.ApiExceptions.BadRequestException;
import org.mule.examples.springboot.restapiwithapikit.api.ApiExceptions.ConflictException;
import org.mule.examples.springboot.restapiwithapikit.api.ApiExceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Spring equivalent of {@code apikit:mapping-exception-strategy name="leagues-apiKitGlobalExceptionMapping"}. */
@RestControllerAdvice
public class ApiKitExceptionMapping {

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<String> notFound() {
        return message(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<String> methodNotAllowed() {
        return message(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<String> unsupportedMediaType() {
        return message(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<String> notAcceptable() {
        return message(HttpStatus.NOT_ACCEPTABLE, "Not acceptable");
    }

    @ExceptionHandler({BadRequestException.class, HttpMessageNotReadableException.class,
            MissingRequestValueException.class})
    ResponseEntity<String> badRequest() {
        return message(HttpStatus.BAD_REQUEST, "Bad request");
    }

    /** No Mule mapping exists for 409 (see README "Behavioural deltas"); answer the RAML-declared status. */
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Void> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    private static ResponseEntity<String> message(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body("{ \"message\": \"" + message + "\" }");
    }
}
