package com.example.leagues.model;

import java.util.Objects;

/**
 * Domain model for a team in the league.
 *
 * <p>Migrated verbatim from the MuleSoft example's
 * {@code org.mule.examples.leagues.model.Team}. Encapsulates a team's
 * standings (points, matches played/won/lost/drawn, goals) and the
 * increment/revert operations used when match results are applied.
 */
public class Team {

    private String id;
    private String name;
    private String homeCity;
    private String stadium;
    private int points;
    private int matchesPlayed;
    private int matchesWon;
    private int matchesLost;
    private int matchesDraw;
    private int goalsInFavor;
    private int goalsAgainst;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHomeCity() {
        return homeCity;
    }

    public void setHomeCity(String homeCity) {
        this.homeCity = homeCity;
    }

    public String getStadium() {
        return stadium;
    }

    public void setStadium(String stadium) {
        this.stadium = stadium;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void incrementMatchesPlayed() {
        matchesPlayed++;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getMatchesWon() {
        return matchesWon;
    }

    public void wonMatch() {
        matchesPlayed++;
        points += 3;
        matchesWon++;
    }

    public void revertWonMatch() {
        matchesPlayed--;
        points -= 3;
        matchesWon--;
    }

    public void drawMatch() {
        matchesPlayed++;
        points += 1;
        matchesDraw++;
    }

    public void revertDrawMatch() {
        matchesPlayed--;
        points -= 1;
        matchesDraw--;
    }

    public void lostMatch() {
        matchesPlayed++;
        matchesLost++;
    }

    public void revertLostMatch() {
        matchesPlayed--;
        matchesLost--;
    }

    public void setMatchesWon(int matchesWon) {
        this.matchesWon = matchesWon;
    }

    public int getMatchesLost() {
        return matchesLost;
    }

    public void setMatchesLost(int matchesLost) {
        this.matchesLost = matchesLost;
    }

    public int getMatchesDraw() {
        return matchesDraw;
    }

    public void setMatchesDraw(int matchesDraw) {
        this.matchesDraw = matchesDraw;
    }

    public void addGoalsInFavor(int goalsInFavor) {
        this.goalsInFavor += goalsInFavor;
    }

    public void addGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst += goalsAgainst;
    }

    public int getGoalsInFavor() {
        return goalsInFavor;
    }

    public void setGoalsInFavor(int goalsInFavor) {
        this.goalsInFavor = goalsInFavor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void setGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Team team = (Team) o;
        return Objects.equals(id, team.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
