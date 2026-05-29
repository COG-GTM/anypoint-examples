package com.example.leagues.exception;

/**
 * Thrown when a requested team or match does not exist.
 *
 * <p>Replaces the MuleSoft APIkit {@code NotFoundException}; mapped to an
 * HTTP 404 response by {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
