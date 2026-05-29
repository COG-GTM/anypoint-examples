package com.example.leagues.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

/**
 * In-memory league holding the teams and the fixture (set of matches).
 *
 * <p>Migrated from the MuleSoft example's
 * {@code org.mule.examples.leagues.model.League}. Two changes were made
 * during migration:
 * <ul>
 *   <li>JSON seed parsing uses Jackson ({@link ObjectMapper}) instead of
 *       {@code org.json.simple}.</li>
 *   <li>Logging uses SLF4J instead of Log4j 1.x.</li>
 * </ul>
 * The fixture-building and standings logic is otherwise identical.
 */
public class League {

    private static final Logger LOGGER = LoggerFactory.getLogger(League.class);

    private static final int MAX_RANDOM_SCORE = 5;
    private static final int MATCH_HOUR = 18;

    private final HashMap<String, Team> teams = new HashMap<>();
    private final HashMap<String, Match> fixture = new HashMap<>();
    private final java.util.Random random = new java.util.Random();

    public void initialize() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("teams.json")) {
            JsonNode root = mapper.readTree(in);
            JsonNode teamsNode = root.get("teams");
            for (JsonNode teamNode : teamsNode) {
                addTeam(teamNode);
            }
            buildFixture();
        } catch (Exception e) {
            LOGGER.error("Error initializing league. Cause: ", e);
        }
    }

    private void buildFixture() {
        Date firstRoundFirstDate = getDate(-7);
        Date firstRoundSecondDate = getDate(-6);
        Date secondRoundFirstDate = getDate(-1);
        Date secondRoundSecondDate = getDate(1);
        int firstRoundGames = 0;
        int secondRoundGames = 0;

        for (Team homeTeam : teams.values()) {
            for (Team awayTeam : teams.values()) {
                if (!homeTeam.equals(awayTeam)) {
                    Match match = new Match();
                    match.setHomeTeam(homeTeam);
                    match.setAwayTeam(awayTeam);
                    if (!playedFirstRound(homeTeam, awayTeam)) {
                        match.setDate((firstRoundGames % 2 == 0) ? firstRoundFirstDate : firstRoundSecondDate);
                        match.setAwayTeamScore(generateRandomScore());
                        match.setHomeTeamScore(generateRandomScore());
                        match.updateResult();
                        firstRoundGames++;
                    } else {
                        match.setDate((secondRoundGames % 2 == 0) ? secondRoundFirstDate : secondRoundSecondDate);
                        secondRoundGames++;
                    }

                    fixture.put(generateFixtureId(homeTeam.getId(), awayTeam.getId()), match);
                }
            }
        }
    }

    private Date getDate(int shiftDays) {
        Calendar date = GregorianCalendar.getInstance();
        date.add(GregorianCalendar.DATE, shiftDays);
        date.set(GregorianCalendar.HOUR_OF_DAY, MATCH_HOUR);
        date.set(GregorianCalendar.MINUTE, 0);
        date.set(GregorianCalendar.SECOND, 0);
        date.set(GregorianCalendar.MILLISECOND, 0);
        return date.getTime();
    }

    private boolean playedFirstRound(Team homeTeam, Team awayTeam) {
        return fixture.containsKey(generateFixtureId(awayTeam.getId(), homeTeam.getId()));
    }

    private String generateFixtureId(String homeTeam, String awayTeam) {
        return homeTeam + ":" + awayTeam;
    }

    public Match getMatch(String homeTeam, String awayTeam) {
        return fixture.get(generateFixtureId(homeTeam, awayTeam));
    }

    public boolean hasMatch(String homeTeam, String awayTeam) {
        return getMatch(homeTeam, awayTeam) != null;
    }

    public List<Match> getMatches() {
        List<Match> matches = new ArrayList<>();
        matches.addAll(fixture.values());
        return matches;
    }

    private int generateRandomScore() {
        return random.nextInt(MAX_RANDOM_SCORE);
    }

    private void addTeam(JsonNode jsonObject) {
        Team team = new Team();
        team.setId(textOrNull(jsonObject, "id"));
        team.setName(textOrNull(jsonObject, "name"));
        team.setHomeCity(textOrNull(jsonObject, "homeCity"));
        team.setStadium(textOrNull(jsonObject, "stadium"));
        this.teams.put(team.getId(), team);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public List<Team> getTeams() {
        List<Team> teams = new ArrayList<>();
        teams.addAll(this.teams.values());
        return teams;
    }

    public List<Team> getTeams(String homeCity) {
        List<Team> teams = getTeams();
        if (homeCity == null) {
            return teams;
        }

        List<Team> teamsByCity = new ArrayList<>();
        for (Team team : teams) {
            if (team.getHomeCity().equals(homeCity)) {
                teamsByCity.add(team);
            }
        }
        return teamsByCity;
    }

    public boolean hasTeam(String id) {
        return teams.containsKey(id);
    }

    public Team getTeam(String id) {
        return teams.get(id);
    }

    public void addTeam(Team team) {
        teams.put(team.getId(), team);
    }

    public void deleteTeam(String id) {
        List<Match> matches = getMatches();
        for (Match match : matches) {
            if (id.equals(match.getHomeTeam().getId()) || id.equals(match.getAwayTeam().getId())) {
                match.revertResult();
                fixture.remove(generateFixtureId(match.getHomeTeam().getId(), match.getAwayTeam().getId()));
            }
        }
        teams.remove(id);
    }

    public List<Team> orderTeamsByPosition() {
        List<Team> teams = getTeams();
        Collections.sort(teams, new Comparator<Team>() {
            public int compare(Team team, Team team2) {
                int byPoints = Integer.valueOf(team2.getPoints()).compareTo(team.getPoints());
                if (byPoints != 0) {
                    return byPoints;
                }

                int byGoalDifference = Integer.valueOf(team2.getGoalsInFavor() - team2.getGoalsAgainst())
                        .compareTo(team.getGoalsInFavor() - team.getGoalsAgainst());

                if (byGoalDifference != 0) {
                    return byGoalDifference;
                }

                return Integer.valueOf(team2.getGoalsInFavor()).compareTo(team.getGoalsInFavor());
            }
        });
        return teams;
    }
}
