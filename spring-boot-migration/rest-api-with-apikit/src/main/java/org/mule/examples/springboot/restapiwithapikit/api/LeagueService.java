package org.mule.examples.springboot.restapiwithapikit.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.mule.examples.springboot.restapiwithapikit.api.ApiExceptions.ConflictException;
import org.mule.examples.springboot.restapiwithapikit.api.ApiExceptions.NotFoundException;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.Fixture;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.MatchResponse;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.Position;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.Positions;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.TeamResponse;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.Teams;
import org.mule.examples.springboot.restapiwithapikit.model.League;
import org.mule.examples.springboot.restapiwithapikit.model.Match;
import org.mule.examples.springboot.restapiwithapikit.model.Team;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Port of {@code org.mule.examples.leagues.LeagueAPI} (the {@code league-api} spring bean, init-method initialize). */
@Service
public class LeagueService {

    private final League league = new League(() -> ThreadLocalRandom.current().nextInt(5));

    public LeagueService() {
        try (InputStream in = new ClassPathResource("teams.json").getInputStream()) {
            league.initialize(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Teams getTeams(String city) {
        List<TeamResponse> teams = new ArrayList<>();
        for (Team t : league.getTeams(city)) {
            teams.add(new TeamResponse(t.getId(), t.getName(), t.getHomeCity(), t.getStadium(), null));
        }
        return new Teams(teams);
    }

    public TeamResponse getTeam(String teamId) {
        Team t = requireTeam(teamId);
        return new TeamResponse(t.getId(), t.getName(), t.getHomeCity(), t.getStadium(), t.getMatchesPlayed());
    }

    public synchronized String addTeam(String id, String name, String homeCity, String stadium) {
        if (league.getTeam(id) != null) {
            throw new ConflictException("There is already a team with id " + id);
        }
        league.addTeam(new Team(id, name, homeCity, stadium));
        return id;
    }

    public synchronized void updateTeam(String teamId, String name, String homeCity, String stadium) {
        Team team = requireTeam(teamId);
        if (name != null) {
            team.setName(name);
        }
        if (homeCity != null) {
            team.setHomeCity(homeCity);
        }
        if (stadium != null) {
            team.setStadium(stadium);
        }
    }

    public synchronized void deleteTeam(String teamId) {
        requireTeam(teamId);
        league.deleteTeam(teamId);
    }

    public Positions getPositions() {
        List<Position> positions = new ArrayList<>();
        int index = 1;
        for (Team t : league.orderTeamsByPosition()) {
            positions.add(new Position(index++, t.getId(), t.getPoints(), t.getMatchesPlayed(), t.getMatchesWon(),
                    t.getMatchesDraw(), t.getMatchesLost(), t.getGoalsInFavor(), t.getGoalsAgainst()));
        }
        return new Positions(positions);
    }

    public Fixture getFixture() {
        return new Fixture(league.getMatches().stream().map(LeagueService::toResponse).toList());
    }

    public MatchResponse getMatch(String homeTeamId, String awayTeamId) {
        return toResponse(requireMatch(homeTeamId, awayTeamId));
    }

    public synchronized void updateScore(String homeTeamId, String awayTeamId, Integer homeScore, Integer awayScore) {
        Match match = requireMatch(homeTeamId, awayTeamId);
        if (new Date().compareTo(match.getDate()) >= 0) {
            match.setHomeTeamScore(homeScore);
            match.setAwayTeamScore(awayScore);
        } else {
            throw new ConflictException("The match between team " + homeTeamId + " and team " + awayTeamId
                    + " has not been played yet");
        }
    }

    private Team requireTeam(String teamId) {
        Team team = league.getTeam(teamId);
        if (team == null) {
            throw new NotFoundException("Team " + teamId + " does not exist");
        }
        return team;
    }

    private Match requireMatch(String homeTeamId, String awayTeamId) {
        Match match = league.getMatch(homeTeamId, awayTeamId);
        if (match == null) {
            throw new NotFoundException("There is no match between team " + homeTeamId + " and team " + awayTeamId);
        }
        return match;
    }

    private static MatchResponse toResponse(Match m) {
        return new MatchResponse(m.getHomeTeam().getId(), m.getAwayTeam().getId(), m.getDate(),
                m.getHomeTeamScore(), m.getAwayTeamScore());
    }
}
