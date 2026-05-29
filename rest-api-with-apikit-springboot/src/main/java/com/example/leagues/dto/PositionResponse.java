package com.example.leagues.dto;

/**
 * Response body for a single standings row.
 *
 * <p>Maps to the MuleSoft example's {@code response.Position}.
 */
public record PositionResponse(
        int position,
        String team,
        int points,
        int matchesPlayed,
        int matchesWon,
        int matchesDraw,
        int matchesLost,
        int goalsInFavor,
        int goalsAgainst) {
}
