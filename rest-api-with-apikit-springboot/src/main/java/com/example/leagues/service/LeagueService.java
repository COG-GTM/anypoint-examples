package com.example.leagues.service;

import com.example.leagues.dto.FixtureResponse;
import com.example.leagues.dto.MatchResponse;
import com.example.leagues.dto.PositionResponse;
import com.example.leagues.dto.PositionsResponse;
import com.example.leagues.dto.ScoreRequest;
import com.example.leagues.dto.TeamRequest;
import com.example.leagues.dto.TeamResponse;
import com.example.leagues.dto.TeamsResponse;
import com.example.leagues.dto.UpdateTeamRequest;
import com.example.leagues.exception.ConflictException;
import com.example.leagues.exception.NotFoundException;
import com.example.leagues.model.League;
import com.example.leagues.model.Match;
import com.example.leagues.model.Team;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Application service holding the league state and exposing the operations
 * that back the REST API.
 *
 * <p>This is the Spring Boot equivalent of the MuleSoft example's
 * {@code LeagueAPI} Spring bean (which was wired into the Mule flows and whose
 * {@code initialize()} method was the bean {@code init-method}). The
 * business logic is preserved; APIkit exceptions are replaced with the
 * application's {@link NotFoundException}/{@link ConflictException}.
 */
@Service
public class LeagueService {

    private final League league = new League();

    @PostConstruct
    public void initialize() {
        league.initialize();
    }

    public TeamsResponse getTeams(String city) {
        List<TeamResponse> teamsList = new ArrayList<>();
        for (Team team : league.getTeams(city)) {
            teamsList.add(new TeamResponse(
                    team.getId(),
                    team.getName(),
                    team.getHomeCity(),
                    team.getStadium(),
                    null));
        }
        return new TeamsResponse(teamsList);
    }

    public TeamResponse getTeam(String teamId) {
        if (!league.hasTeam(teamId)) {
            throw new NotFoundException("Team " + teamId + " does not exist");
        }
        Team team = league.getTeam(teamId);
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getHomeCity(),
                team.getStadium(),
                team.getMatchesPlayed());
    }

    public String addTeam(TeamRequest team) {
        if (league.hasTeam(team.getId())) {
            throw new ConflictException("There is already a team with id " + team.getId());
        }
        Team newTeam = new Team();
        newTeam.setId(team.getId());
        newTeam.setName(team.getName());
        newTeam.setHomeCity(team.getHomeCity());
        newTeam.setStadium(team.getStadium());
        league.addTeam(newTeam);
        return team.getId();
    }

    public void updateTeam(String teamId, UpdateTeamRequest updateTeam) {
        if (!league.hasTeam(teamId)) {
            throw new NotFoundException("Team " + teamId + " does not exist");
        }
        Team team = league.getTeam(teamId);
        if (updateTeam.getName() != null) {
            team.setName(updateTeam.getName());
        }
        if (updateTeam.getHomeCity() != null) {
            team.setHomeCity(updateTeam.getHomeCity());
        }
        if (updateTeam.getStadium() != null) {
            team.setStadium(updateTeam.getStadium());
        }
    }

    public void deleteTeam(String teamId) {
        if (!league.hasTeam(teamId)) {
            throw new NotFoundException("Team " + teamId + " does not exist");
        }
        league.deleteTeam(teamId);
    }

    public PositionsResponse getPositions() {
        List<PositionResponse> positionsList = new ArrayList<>();
        int positionIndex = 1;
        for (Team team : league.orderTeamsByPosition()) {
            positionsList.add(new PositionResponse(
                    positionIndex,
                    team.getId(),
                    team.getPoints(),
                    team.getMatchesPlayed(),
                    team.getMatchesWon(),
                    team.getMatchesDraw(),
                    team.getMatchesLost(),
                    team.getGoalsInFavor(),
                    team.getGoalsAgainst()));
            positionIndex++;
        }
        return new PositionsResponse(positionsList);
    }

    public FixtureResponse getFixture() {
        List<MatchResponse> matchesList = new ArrayList<>();
        for (Match match : league.getMatches()) {
            matchesList.add(toMatchResponse(match));
        }
        return new FixtureResponse(matchesList);
    }

    public MatchResponse getMatch(String homeTeamId, String awayTeamId) {
        if (!league.hasMatch(homeTeamId, awayTeamId)) {
            throw new NotFoundException(
                    "There is no match between team " + homeTeamId + " and team " + awayTeamId);
        }
        return toMatchResponse(league.getMatch(homeTeamId, awayTeamId));
    }

    public void updateScore(String homeTeamId, String awayTeamId, ScoreRequest score) {
        if (!league.hasMatch(homeTeamId, awayTeamId)) {
            throw new NotFoundException(
                    "There is no match between team " + homeTeamId + " and team " + awayTeamId);
        }
        Match match = league.getMatch(homeTeamId, awayTeamId);
        if (new Date().compareTo(match.getDate()) >= 0) {
            match.setHomeTeamScore(score.getHomeTeamScore());
            match.setAwayTeamScore(score.getAwayTeamScore());
        } else {
            throw new ConflictException("The match between team " + homeTeamId
                    + " and team " + awayTeamId + " has not been played yet");
        }
    }

    private MatchResponse toMatchResponse(Match match) {
        return new MatchResponse(
                match.getHomeTeam().getId(),
                match.getAwayTeam().getId(),
                match.getDate(),
                match.getHomeTeamScore(),
                match.getAwayTeamScore());
    }
}
