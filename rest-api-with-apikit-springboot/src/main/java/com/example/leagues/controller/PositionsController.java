package com.example.leagues.controller;

import com.example.leagues.dto.PositionsResponse;
import com.example.leagues.service.LeagueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the {@code /positions} resource (league standings).
 *
 * <p>Replaces the APIkit flow mapping for {@code /positions} (get) from the
 * MuleSoft {@code rest-api-with-apikit.xml}.
 */
@RestController
@RequestMapping("/api/positions")
public class PositionsController {

    private final LeagueService leagueService;

    public PositionsController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping
    public PositionsResponse getPositions() {
        return leagueService.getPositions();
    }
}
