package com.example.leagues.dto;

/**
 * Request body for updating a match score
 * (PUT /api/fixture/{homeTeamId}/{awayTeamId}).
 *
 * <p>Maps to the MuleSoft example's {@code request.Score}.
 */
public class ScoreRequest {

    private int homeTeamScore;
    private int awayTeamScore;

    public int getHomeTeamScore() {
        return homeTeamScore;
    }

    public void setHomeTeamScore(int homeTeamScore) {
        this.homeTeamScore = homeTeamScore;
    }

    public int getAwayTeamScore() {
        return awayTeamScore;
    }

    public void setAwayTeamScore(int awayTeamScore) {
        this.awayTeamScore = awayTeamScore;
    }
}
