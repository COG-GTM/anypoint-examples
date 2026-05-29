package com.example.leagues.controller;

import com.example.leagues.dto.FixtureResponse;
import com.example.leagues.dto.MatchResponse;
import com.example.leagues.dto.ScoreRequest;
import com.example.leagues.service.LeagueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the {@code /fixture} resource.
 *
 * <p>Replaces the APIkit flow mappings for {@code /fixture} and
 * {@code /fixture/{homeTeamId}/{awayTeamId}} (get/get/put) from the MuleSoft
 * {@code rest-api-with-apikit.xml}.
 */
@RestController
@RequestMapping("/api/fixture")
public class FixtureController {

    private final LeagueService leagueService;

    public FixtureController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping
    public FixtureResponse getFixture() {
        return leagueService.getFixture();
    }

    @GetMapping("/{homeTeamId}/{awayTeamId}")
    public MatchResponse getMatch(@PathVariable String homeTeamId,
                                  @PathVariable String awayTeamId) {
        return leagueService.getMatch(homeTeamId, awayTeamId);
    }

    @PutMapping("/{homeTeamId}/{awayTeamId}")
    public ResponseEntity<Void> updateScore(@PathVariable String homeTeamId,
                                            @PathVariable String awayTeamId,
                                            @RequestBody ScoreRequest score) {
        leagueService.updateScore(homeTeamId, awayTeamId, score);
        return ResponseEntity.noContent().build();
    }
}
