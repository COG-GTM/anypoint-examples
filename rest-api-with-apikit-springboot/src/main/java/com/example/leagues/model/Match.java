package com.example.leagues.model;

import java.util.Date;
import java.util.Objects;

/**
 * Domain model for a match between two teams.
 *
 * <p>Migrated verbatim from the MuleSoft example's
 * {@code org.mule.examples.leagues.model.Match}. Applying or reverting a
 * result mutates both teams' standings via {@link Team}.
 */
public class Match {

    private Team homeTeam;
    private Team awayTeam;
    private Date date;
    private Integer homeTeamScore;
    private Integer awayTeamScore;

    public Team getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Integer getHomeTeamScore() {
        return homeTeamScore;
    }

    public void setHomeTeamScore(Integer homeTeamScore) {
        this.homeTeamScore = homeTeamScore;
    }

    public Integer getAwayTeamScore() {
        return awayTeamScore;
    }

    public void setAwayTeamScore(Integer awayTeamScore) {
        this.awayTeamScore = awayTeamScore;
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
        // Only revert the result if they've played
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Match match = (Match) o;
        return Objects.equals(awayTeam, match.awayTeam)
                && Objects.equals(awayTeamScore, match.awayTeamScore)
                && Objects.equals(date, match.date)
                && Objects.equals(homeTeam, match.homeTeam)
                && Objects.equals(homeTeamScore, match.homeTeamScore);
    }

    @Override
    public int hashCode() {
        int result = homeTeam != null ? homeTeam.hashCode() : 0;
        result = 31 * result + (awayTeam != null ? awayTeam.hashCode() : 0);
        return result;
    }
}
