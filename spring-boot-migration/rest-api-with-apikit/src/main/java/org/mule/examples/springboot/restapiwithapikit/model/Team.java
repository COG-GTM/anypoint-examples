package org.mule.examples.springboot.restapiwithapikit.model;

/** Port of {@code org.mule.examples.leagues.model.Team}. */
public class Team {

    private final String id;
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

    public Team(String id, String name, String homeCity, String stadium) {
        this.id = id;
        this.name = name;
        this.homeCity = homeCity;
        this.stadium = stadium;
    }

    public void wonMatch() { matchesPlayed++; points += 3; matchesWon++; }
    public void revertWonMatch() { matchesPlayed--; points -= 3; matchesWon--; }
    public void drawMatch() { matchesPlayed++; points += 1; matchesDraw++; }
    public void revertDrawMatch() { matchesPlayed--; points -= 1; matchesDraw--; }
    public void lostMatch() { matchesPlayed++; matchesLost++; }
    public void revertLostMatch() { matchesPlayed--; matchesLost--; }
    public void addGoalsInFavor(int goals) { goalsInFavor += goals; }
    public void addGoalsAgainst(int goals) { goalsAgainst += goals; }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHomeCity() { return homeCity; }
    public void setHomeCity(String homeCity) { this.homeCity = homeCity; }
    public String getStadium() { return stadium; }
    public void setStadium(String stadium) { this.stadium = stadium; }
    public int getPoints() { return points; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public int getMatchesWon() { return matchesWon; }
    public int getMatchesLost() { return matchesLost; }
    public int getMatchesDraw() { return matchesDraw; }
    public int getGoalsInFavor() { return goalsInFavor; }
    public int getGoalsAgainst() { return goalsAgainst; }
}
