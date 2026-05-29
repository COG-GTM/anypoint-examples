package com.example.leagues.exception;

/**
 * Thrown when an operation conflicts with current state, e.g. adding a team
 * that already exists or updating the score of a match that has not been
 * played yet.
 *
 * <p>Replaces the MuleSoft example's {@code ConflictException}; mapped to an
 * HTTP 409 response by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
