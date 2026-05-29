package com.example.leagues.dto;

import java.util.List;

/**
 * Response body for the teams collection (GET /api/teams).
 *
 * <p>Maps to the MuleSoft example's {@code response.Teams}.
 */
public record TeamsResponse(List<TeamResponse> teams) {
}
