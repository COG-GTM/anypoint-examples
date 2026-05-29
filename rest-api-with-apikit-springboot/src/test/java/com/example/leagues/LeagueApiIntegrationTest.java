package com.example.leagues;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the migrated API, exercising the same behaviours the
 * original MuleSoft application exposed. Each test runs against a freshly
 * initialized league (fixture is seeded with random scores at startup), so
 * assertions avoid depending on specific generated scores.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LeagueApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTeamsReturnsAllSeededTeams() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams", hasSize(20)));
    }

    @Test
    void getTeamsFilteredByCity() throws Exception {
        // Madrid is home to ATL, RAY and RMA in the seed data.
        mockMvc.perform(get("/api/teams").param("city", "Madrid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams", hasSize(3)));
    }

    @Test
    void getSingleTeamReturnsDetails() throws Exception {
        mockMvc.perform(get("/api/teams/BAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("BAR")))
                .andExpect(jsonPath("$.name", is("Barcelona")))
                .andExpect(jsonPath("$.homeCity", is("Barcelona")));
    }

    @Test
    void getUnknownTeamReturns404() throws Exception {
        mockMvc.perform(get("/api/teams/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message", is("Resource not found")));
    }

    @Test
    void createTeamReturns201WithLocation() throws Exception {
        String body = "{\"id\":\"TST\",\"name\":\"Test FC\",\"homeCity\":\"Testville\",\"stadium\":\"Test Arena\"}";
        mockMvc.perform(post("/api/teams").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/teams/TST")));

        mockMvc.perform(get("/api/teams/TST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test FC")));
    }

    @Test
    void createDuplicateTeamReturns409() throws Exception {
        String body = "{\"id\":\"BAR\",\"name\":\"Barcelona\",\"homeCity\":\"Barcelona\"}";
        mockMvc.perform(post("/api/teams").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createInvalidTeamReturns400() throws Exception {
        // Missing required "name" field violates Bean Validation.
        String body = "{\"id\":\"NEW\",\"homeCity\":\"Nowhere\"}";
        mockMvc.perform(post("/api/teams").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request")));
    }

    @Test
    void deleteTeamReturns204ThenNotFound() throws Exception {
        mockMvc.perform(delete("/api/teams/BAR"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/teams/BAR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPositionsReturnsStandings() throws Exception {
        mockMvc.perform(get("/api/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions", hasSize(20)))
                .andExpect(jsonPath("$.positions[0].position", is(1)));
    }

    @Test
    void getFixtureReturnsMatches() throws Exception {
        mockMvc.perform(get("/api/fixture"))
                .andExpect(status().isOk())
                // 20 teams playing home & away => 20 * 19 = 380 matches.
                .andExpect(jsonPath("$.fixture", hasSize(380)));
    }

    @Test
    void getMatchBetweenTwoTeams() throws Exception {
        mockMvc.perform(get("/api/fixture/BAR/RMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeTeam", is("BAR")))
                .andExpect(jsonPath("$.awayTeam", is("RMA")));
    }

    @Test
    void methodNotAllowedReturns405() throws Exception {
        // /api/positions only supports GET.
        mockMvc.perform(post("/api/positions").contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", is("Method not allowed")));
    }
}
