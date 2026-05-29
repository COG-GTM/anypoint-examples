package com.example.leagues.controller;

import com.example.leagues.dto.TeamRequest;
import com.example.leagues.dto.TeamResponse;
import com.example.leagues.dto.TeamsResponse;
import com.example.leagues.dto.UpdateTeamRequest;
import com.example.leagues.service.LeagueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for the {@code /teams} resource.
 *
 * <p>Replaces the APIkit flow mappings for {@code /teams} and
 * {@code /teams/{teamId}} (get/post/get/put/delete) from the MuleSoft
 * {@code rest-api-with-apikit.xml}.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamsController {

    private final LeagueService leagueService;

    public TeamsController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping
    public TeamsResponse getTeams(@RequestParam(value = "city", required = false) String city) {
        return leagueService.getTeams(city);
    }

    @PostMapping
    public ResponseEntity<Void> addTeam(@Valid @RequestBody TeamRequest team) {
        String id = leagueService.addTeam(team);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{teamId}")
    public TeamResponse getTeam(@PathVariable String teamId) {
        return leagueService.getTeam(teamId);
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<Void> updateTeam(@PathVariable String teamId,
                                           @RequestBody UpdateTeamRequest updateTeam) {
        leagueService.updateTeam(teamId, updateTeam);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String teamId) {
        leagueService.deleteTeam(teamId);
        return ResponseEntity.noContent().build();
    }
}
