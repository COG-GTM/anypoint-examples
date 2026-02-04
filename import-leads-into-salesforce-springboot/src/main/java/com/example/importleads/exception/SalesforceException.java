package com.example.importleads.exception;

public class SalesforceException extends RuntimeException {

    public SalesforceException(String message) {
        super(message);
    }

    public SalesforceException(String message, Throwable cause) {
        super(message, cause);
    }
}
