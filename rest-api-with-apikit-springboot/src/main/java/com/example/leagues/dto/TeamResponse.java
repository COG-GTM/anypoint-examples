package com.example.leagues.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response body for a single team.
 *
 * <p>Maps to the MuleSoft example's {@code response.Team}. {@code matches} is
 * only populated for the single-team view; null fields are omitted to match
 * the original {@code NON_NULL} serialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamResponse(
        String id,
        String name,
        String homeCity,
        String stadium,
        Integer matches) {
}
