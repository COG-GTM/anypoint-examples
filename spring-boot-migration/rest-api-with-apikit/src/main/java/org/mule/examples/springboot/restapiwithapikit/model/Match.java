package org.mule.examples.springboot.restapiwithapikit.model;

import java.util.Date;

/** Port of {@code org.mule.examples.leagues.model.Match}. */
public class Match {

    private final Team homeTeam;
    private final Team awayTeam;
    private Date date;
    private Integer homeTeamScore;
    private Integer awayTeamScore;

    public Match(Team homeTeam, Team awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public void updateResult() {
        if (homeTeamScore > awayTeamScore) {
            homeTeam.wonMatch();
            awayTeam.lostMatch();
        } else if (homeTeamScore < awayTeamScore) {
            homeTeam.lostMatch();
            awayTeam.wonMatch();
        } else {
            homeTeam.drawMatch();
            awayTeam.drawMatch();
        }
        homeTeam.addGoalsInFavor(homeTeamScore);
        homeTeam.addGoalsAgainst(awayTeamScore);
        awayTeam.addGoalsInFavor(awayTeamScore);
        awayTeam.addGoalsAgainst(homeTeamScore);
    }

    public void revertResult() {
        if (homeTeamScore != null && awayTeamScore != null) {
            if (homeTeamScore > awayTeamScore) {
                homeTeam.revertWonMatch();
                awayTeam.revertLostMatch();
            } else if (homeTeamScore < awayTeamScore) {
                homeTeam.revertLostMatch();
                awayTeam.revertWonMatch();
            } else {
                homeTeam.revertDrawMatch();
                awayTeam.revertDrawMatch();
            }
            homeTeam.addGoalsInFavor(-homeTeamScore);
            homeTeam.addGoalsAgainst(-awayTeamScore);
            awayTeam.addGoalsInFavor(-awayTeamScore);
            awayTeam.addGoalsAgainst(-homeTeamScore);
        }
    }

    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public Integer getHomeTeamScore() { return homeTeamScore; }
    public void setHomeTeamScore(Integer homeTeamScore) { this.homeTeamScore = homeTeamScore; }
    public Integer getAwayTeamScore() { return awayTeamScore; }
    public void setAwayTeamScore(Integer awayTeamScore) { this.awayTeamScore = awayTeamScore; }
}
