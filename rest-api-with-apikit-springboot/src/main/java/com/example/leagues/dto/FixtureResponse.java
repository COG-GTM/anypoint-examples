package com.example.leagues.dto;

import java.util.List;

/**
 * Response body for the fixture (GET /api/fixture).
 *
 * <p>Maps to the MuleSoft example's {@code response.Fixture}.
 */
public record FixtureResponse(List<MatchResponse> fixture) {
}
