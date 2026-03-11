/**
 * MuleSoft Examples
 * Copyright 2014 MuleSoft, Inc.
 *
 * This product includes software developed at
 * MuleSoft, Inc. (http://www.mulesoft.com/).
 */

package org.mule.examples.leagues.model;

import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class League {

    private static final Logger LOGGER = Logger.getLogger(League.class);

    private static final int MAX_RANDOM_SCORE = 5;
    private static final int MATCH_HOUR = 18;

    private HashMap<String, Team> teams = new HashMap<>();
    private HashMap<String, Match> fixture = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void initialize() {
        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("teams.json")));
            JSONArray teams = (JSONArray) jsonObject.get("teams");
            teams.forEach(team -> addTeam((JSONObject) team));
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

        for(Team homeTeam : teams.values()) {
            for(Team awayTeam : teams.values()) {
                if(!homeTeam.equals(awayTeam)) {
                    Match match = new Match();
                    match.setHomeTeam(homeTeam);
                    match.setAwayTeam(awayTeam);
                    if(!playedFirstRound(homeTeam, awayTeam)) {
                        match.setDate((firstRoundGames % 2 == 0)? firstRoundFirstDate : firstRoundSecondDate);
                        match.setAwayTeamScore(generateRandomScore());
                        match.setHomeTeamScore(generateRandomScore());
                        match.updateResult();
                        firstRoundGames++;
                    } else {
                        match.setDate((secondRoundGames % 2 == 0)? secondRoundFirstDate : secondRoundSecondDate);
                        secondRoundGames++;
                    }

                    fixture.put(generateFixtureId(homeTeam.getId(), awayTeam.getId()), match);
                }
            }
        }
    }

    private Date getDate(int shiftDays) {
        LocalDateTime dateTime = LocalDateTime.now()
                .plusDays(shiftDays)
                .withHour(MATCH_HOUR)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
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
        return new ArrayList<>(fixture.values());
    }

    private int generateRandomScore() {
        return new Random().nextInt(MAX_RANDOM_SCORE);
    }

    private void addTeam(JSONObject jsonObject) {
        Team team = new Team();
        team.setId((String) jsonObject.get("id"));
        team.setName((String) jsonObject.get("name"));
        team.setHomeCity((String) jsonObject.get("homeCity"));
        team.setStadium((String) jsonObject.get("stadium"));
        this.teams.put(team.getId(), team);
    }

    public List<Team> getTeams() {
        return new ArrayList<>(this.teams.values());
    }

    public List<Team> getTeams(String homeCity) {
        List<Team> teams = getTeams();
        if(homeCity == null) {
            return teams;
        }

        return teams.stream()
                .filter(team -> team.getHomeCity().equals(homeCity))
                .collect(Collectors.toList());
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
        for(Match match : matches) {
            if(id.equals(match.getHomeTeam().getId()) || id.equals(match.getAwayTeam().getId())) {
                match.revertResult();
                fixture.remove(generateFixtureId(match.getHomeTeam().getId(), match.getAwayTeam().getId()));
            }
        }
        teams.remove(id);
    }

    public List<Team> orderTeamsByPosition() {
        List<Team> teams = getTeams();
        teams.sort(Comparator.comparingInt(Team::getPoints).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.getGoalsInFavor() - t.getGoalsAgainst()).reversed())
                .thenComparing(Comparator.comparingInt(Team::getGoalsInFavor).reversed()));
        return teams;
    }

}
