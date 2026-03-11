/**
 * MuleSoft Examples
 * Copyright 2014 MuleSoft, Inc.
 *
 * This product includes software developed at
 * MuleSoft, Inc. (http://www.mulesoft.com/).
 */

package org.mule.examples.leagues;

import org.mule.examples.leagues.exceptions.ConflictException;
import org.mule.examples.leagues.model.League;
import org.mule.examples.leagues.request.Score;
import org.mule.examples.leagues.request.UpdateTeam;
import org.mule.examples.leagues.response.Fixture;
import org.mule.examples.leagues.response.Match;
import org.mule.examples.leagues.response.Position;
import org.mule.examples.leagues.response.Positions;
import org.mule.examples.leagues.response.Team;
import org.mule.examples.leagues.response.Teams;
import org.mule.module.apikit.exception.MuleRestException;
import org.mule.module.apikit.exception.NotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LeagueAPI {

    private League league;

    public void initialize() {
        league = new League();
        league.initialize();
    }


    public void deleteTeam(String teamId) throws MuleRestException {
        if(!league.hasTeam(teamId)) {
            throw new NotFoundException("Team " + teamId + " does not exist");
        }
        league.deleteTeam(teamId);
    }

    public Teams getTeams(String city) {
        List<Team> teamsList = league.getTeams(city).stream()
                .map(team -> {
                    Team teamResp = new Team();
                    teamResp.setId(team.getId());
                    teamResp.setName(team.getName());
                    teamResp.setHomeCity(team.getHomeCity());
                    teamResp.setStadium(team.getStadium());
                    return teamResp;
                })
                .collect(Collectors.toList());

        Teams teamsResp = new Teams();
        teamsResp.setTeams(teamsList);
        return teamsResp;
    }

    public Match getMatch(String homeTeamId, String awayTeamId) throws MuleRestException {
        if(!league.hasMatch(homeTeamId, awayTeamId)) {
            throw new NotFoundException("There is no match between team " + homeTeamId + " and team " + awayTeamId);
        }

        org.mule.examples.leagues.model.Match match = league.getMatch(homeTeamId, awayTeamId);
        Match matchResp = new Match();
        matchResp.setHomeTeam(match.getHomeTeam().getId());
        matchResp.setAwayTeam(match.getAwayTeam().getId());
        matchResp.setDate(match.getDate());
        matchResp.setHomeTeamScore(match.getHomeTeamScore());
        matchResp.setAwayTeamScore(match.getAwayTeamScore());
        return matchResp;
    }

    public Team getTeam(String teamId) throws MuleRestException {
        if(!league.hasTeam(teamId)) {
            throw new NotFoundException("Team " + teamId + " does not exist");
        }

        org.mule.examples.leagues.model.Team team = league.getTeam(teamId);
        Team teamResp = new Team();
        teamResp.setId(team.getId());
        teamResp.setName(team.getName());
        teamResp.setHomeCity(team.getHomeCity());
        teamResp.setStadium(team.getStadium());
        teamResp.setMatches(team.getMatchesPlayed());
        return teamResp;
    }

    public Positions getPositions() throws MuleRestException {
        Positions positions = new Positions();
        AtomicInteger positionIndex = new AtomicInteger(1);

        List<Position> positionsList = league.orderTeamsByPosition().stream()
                .map(team -> {
                    Position position = new Position();
                    position.setPosition(positionIndex.getAndIncrement());
                    position.setTeam(team.getId());
                    position.setPoints(team.getPoints());
                    position.setMatchesPlayed(team.getMatchesPlayed());
                    position.setMatchesWon(team.getMatchesWon());
                    position.setMatchesLost(team.getMatchesLost());
                    position.setMatchesDraw(team.getMatchesDraw());
                    position.setGoalsAgainst(team.getGoalsAgainst());
                    position.setGoalsInFavor(team.getGoalsInFavor());
                    return position;
                })
                .collect(Collectors.toList());

        positions.setPositions(positionsList);
        return positions;
    }

    public Fixture getFixture() {
        Fixture fixture = new Fixture();
        List<Match> matchesList = league.getMatches().stream()
                .map(match -> {
                    Match matchResp = new Match();
                    matchResp.setHomeTeam(match.getHomeTeam().getId());
                    matchResp.setAwayTeam(match.getAwayTeam().getId());
                    matchResp.setDate(match.getDate());
                    matchResp.setHomeTeamScore(match.getHomeTeamScore());
                    matchResp.setAwayTeamScore(match.getAwayTeamScore());
                    return matchResp;
                })
                .collect(Collectors.toList());

        fixture.setFixture(matchesList);
        return fixture;
    }

    public String addTeam(org.mule.examples.leagues.request.Team team) throws MuleRestException {
        if(league.hasTeam(team.getId())) {
            throw new ConflictException("There is already a team with id " + team.getId());
        }

        org.mule.examples.leagues.model.Team newTeam = new org.mule.examples.leagues.model.Team();
        newTeam.setId(team.getId());
        newTeam.setName(team.getName());
        newTeam.setHomeCity(team.getHomeCity());
        newTeam.setStadium(team.getStadium());

        league.addTeam(newTeam);
        return team.getId();
    }

    public void updateScore(String homeTeamId, String awayTeamId, Score score) throws MuleRestException {
        if(!league.hasMatch(homeTeamId, awayTeamId)) {
            throw new NotFoundException("There is no match between team " + homeTeamId + " and team " + awayTeamId);
        }

        org.mule.examples.leagues.model.Match match = league.getMatch(homeTeamId, awayTeamId);
        if(new Date().compareTo(match.getDate()) >= 0) {
            match.setHomeTeamScore(score.getHomeTeamScore());
            match.setAwayTeamScore(score.getAwayTeamScore());
        } else {
            throw new ConflictException("The match between team " + homeTeamId + " and team " + awayTeamId + " has not been played yet");
        }
    }

    public void updateTeam(String teamId, UpdateTeam updateTeam) throws MuleRestException {
        if(!league.hasTeam(teamId)) {
            throw new NotFoundException("Team " + teamId + " does not exist");
        }

        org.mule.examples.leagues.model.Team team = league.getTeam(teamId);
        if(updateTeam.getName() != null) {
            team.setName(updateTeam.getName());
        }

        if(updateTeam.getHomeCity() != null) {
            team.setHomeCity(updateTeam.getHomeCity());
        }

        if(updateTeam.getStadium() != null) {
            team.setStadium(updateTeam.getStadium());
        }
    }

}
