package org.mule.examples.springboot.restapiwithapikit.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.Fixture;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.MatchResponse;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.Positions;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.TeamResponse;
import org.mule.examples.springboot.restapiwithapikit.api.ApiResponses.Teams;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The resources of leagues.raml. The servlet context path {@code /api} replaces the Mule listener path {@code /api/*};
 * each method replaces one APIkit-generated flow ({@code <method>:/<resource>:leagues-config}).
 */
@RestController
@Tag(name = "La Liga")
public class LeagueController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final LeagueService league;
    private final Environment environment;

    public LeagueController(LeagueService league, Environment environment) {
        this.league = league;
        this.environment = environment;
    }

    @Operation(summary = "Obtain information from a collection of teams simultaneously")
    @GetMapping(path = "/teams", produces = JSON)
    public Teams getTeams(@RequestParam(required = false) String city) {
        return league.getTeams(city);
    }

    @Operation(summary = "Add a new team to the league")
    @PostMapping(path = "/teams", consumes = JSON)
    public ResponseEntity<Void> addTeam(@RequestBody JsonNode body) {
        RamlValidator.object(body);
        String id = RamlValidator.teamId(body);
        String name = RamlValidator.optionalString(body, "name");
        String homeCity = RamlValidator.optionalString(body, "homeCity");
        String stadium = RamlValidator.optionalString(body, "stadium");
        league.addTeam(id, name, homeCity, stadium);
        return ResponseEntity.created(URI.create(
                "http://localhost:" + httpPort() + "/api/teams/" + id)).build();
    }

    @Operation(summary = "Retrieve team-related information")
    @GetMapping(path = "/teams/{teamId}", produces = JSON)
    public TeamResponse getTeam(@PathVariable String teamId) {
        return league.getTeam(RamlValidator.teamCode("teamId", teamId));
    }

    @Operation(summary = "Update team details")
    @PutMapping(path = "/teams/{teamId}", consumes = JSON)
    public ResponseEntity<Void> updateTeam(@PathVariable String teamId, @RequestBody JsonNode body) {
        RamlValidator.teamCode("teamId", teamId);
        RamlValidator.object(body);
        league.updateTeam(teamId, RamlValidator.optionalString(body, "name"),
                RamlValidator.optionalString(body, "homeCity"), RamlValidator.optionalString(body, "stadium"));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove a team from the league")
    @DeleteMapping(path = "/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String teamId) {
        league.deleteTeam(RamlValidator.teamCode("teamId", teamId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Retrieve the current standing for the current season")
    @GetMapping(path = "/positions", produces = JSON)
    public Positions getPositions() {
        return league.getPositions();
    }

    @Operation(summary = "Retrieve a list of matches for the current season")
    @GetMapping(path = "/fixture", produces = JSON)
    public Fixture getFixture() {
        return league.getFixture();
    }

    @Operation(summary = "Retrieve details of the match between the two teams")
    @GetMapping(path = "/fixture/{homeTeamId}/{awayTeamId}", produces = JSON)
    public MatchResponse getMatch(@PathVariable String homeTeamId, @PathVariable String awayTeamId) {
        return league.getMatch(RamlValidator.teamCode("homeTeamId", homeTeamId),
                RamlValidator.teamCode("awayTeamId", awayTeamId));
    }

    @Operation(summary = "Update match results. Can only be done after the game has ended.")
    @PutMapping(path = "/fixture/{homeTeamId}/{awayTeamId}", consumes = JSON)
    public ResponseEntity<Void> updateScore(@PathVariable String homeTeamId, @PathVariable String awayTeamId,
                                            @RequestBody JsonNode body) {
        RamlValidator.teamCode("homeTeamId", homeTeamId);
        RamlValidator.teamCode("awayTeamId", awayTeamId);
        RamlValidator.object(body);
        league.updateScore(homeTeamId, awayTeamId, RamlValidator.integerOrZero(body, "homeTeamScore"),
                RamlValidator.integerOrZero(body, "awayTeamScore"));
        return ResponseEntity.noContent().build();
    }

    /** Mule resolves {@code ${http.port}} in the Location header; use the port the server actually listens on. */
    private String httpPort() {
        return environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
    }
}
