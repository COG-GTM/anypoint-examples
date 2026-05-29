package com.example.leagues.dto;

/**
 * Standard JSON error body: {@code { "message": "..." }}.
 *
 * <p>Mirrors the JSON error payloads produced by the MuleSoft APIkit
 * {@code mapping-exception-strategy}.
 */
public record ErrorResponse(String message) {
}
