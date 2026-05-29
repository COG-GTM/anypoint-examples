package com.example.leagues.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

/**
 * Response body for a single match.
 *
 * <p>Maps to the MuleSoft example's {@code response.Match}. The date is
 * serialized as ISO 8601 ({@code yyyy-MM-dd'T'HH:mm:ssZ}), replicating the
 * original {@code JsonDateSerializer}. Null fields (date/scores for matches
 * not yet played) are omitted to match the {@code NON_NULL} serialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchResponse(
        String homeTeam,
        String awayTeam,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
        Date date,
        Integer homeTeamScore,
        Integer awayTeamScore) {
}
