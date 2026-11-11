package org.mule.examples.springboot.restapiwithapikit.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

/**
 * Port of {@code org.mule.examples.leagues.model.League}. Keeps the same {@link HashMap}s and insertion order so the
 * generated fixture (which pairs are first-round, which second-round matches are past/future) is the same as Mule's.
 */
public class League {

    static final int MAX_RANDOM_SCORE = 5;
    private static final int MATCH_HOUR = 18;

    private final Map<String, Team> teams = new HashMap<>();
    private final Map<String, Match> fixture = new HashMap<>();
    private final IntSupplier randomScore;

    public League(IntSupplier randomScore) {
        this.randomScore = randomScore;
    }

    public void initialize(InputStream teamsJson) {
        try {
            for (JsonNode t : new ObjectMapper().readTree(teamsJson).get("teams")) {
                addTeam(new Team(text(t, "id"), text(t, "name"), text(t, "homeCity"), text(t, "stadium")));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error initializing league", e);
        }
        buildFixture();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
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
                if (!homeTeam.getId().equals(awayTeam.getId())) {
                    Match match = new Match(homeTeam, awayTeam);
                    if (!fixture.containsKey(fixtureId(awayTeam.getId(), homeTeam.getId()))) {
                        match.setDate(firstRoundGames % 2 == 0 ? firstRoundFirstDate : firstRoundSecondDate);
                        match.setAwayTeamScore(randomScore.getAsInt());
                        match.setHomeTeamScore(randomScore.getAsInt());
                        match.updateResult();
                        firstRoundGames++;
                    } else {
                        match.setDate(secondRoundGames % 2 == 0 ? secondRoundFirstDate : secondRoundSecondDate);
                        secondRoundGames++;
                    }
                    fixture.put(fixtureId(homeTeam.getId(), awayTeam.getId()), match);
                }
            }
        }
    }

    private static Date getDate(int shiftDays) {
        Calendar date = GregorianCalendar.getInstance();
        date.add(Calendar.DATE, shiftDays);
        date.set(Calendar.HOUR_OF_DAY, MATCH_HOUR);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTime();
    }

    private static String fixtureId(String homeTeam, String awayTeam) {
        return homeTeam + ":" + awayTeam;
    }

    public synchronized Match getMatch(String homeTeam, String awayTeam) {
        return fixture.get(fixtureId(homeTeam, awayTeam));
    }

    public synchronized List<Match> getMatches() {
        return new ArrayList<>(fixture.values());
    }

    public synchronized List<Team> getTeams(String homeCity) {
        List<Team> result = new ArrayList<>();
        for (Team team : teams.values()) {
            if (homeCity == null || team.getHomeCity().equals(homeCity)) {
                result.add(team);
            }
        }
        return result;
    }

    public synchronized Team getTeam(String id) {
        return teams.get(id);
    }

    public synchronized void addTeam(Team team) {
        teams.put(team.getId(), team);
    }

    public synchronized void deleteTeam(String id) {
        for (Match match : getMatches()) {
            if (id.equals(match.getHomeTeam().getId()) || id.equals(match.getAwayTeam().getId())) {
                match.revertResult();
                fixture.remove(fixtureId(match.getHomeTeam().getId(), match.getAwayTeam().getId()));
            }
        }
        teams.remove(id);
    }

    public synchronized List<Team> orderTeamsByPosition() {
        List<Team> ordered = getTeams(null);
        ordered.sort(Comparator.comparingInt(Team::getPoints).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.getGoalsInFavor() - t.getGoalsAgainst()).reversed())
                .thenComparing(Comparator.comparingInt(Team::getGoalsInFavor).reversed()));
        return ordered;
    }
}
