package com.example.leagues.dto;

import java.util.List;

/**
 * Response body for the standings table (GET /api/positions).
 *
 * <p>Maps to the MuleSoft example's {@code response.Positions}.
 */
public record PositionsResponse(List<PositionResponse> positions) {
}
