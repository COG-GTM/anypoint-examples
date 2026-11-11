package org.mule.examples.springboot.restapiwithapikit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * GET /fixture and GET /positions contain random first-round scores and dates relative to "now" (League.buildFixture),
 * so they can't be fixed fixtures. These checks pin the deterministic parts of that Mule behaviour instead.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeagueInvariantsTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void fixtureIsADoubleRoundRobinWithScoresOnlyForFirstRound() {
        JsonNode fixture = rest.getForObject("/fixture", JsonNode.class).get("fixture");
        assertThat(fixture).hasSize(20 * 19);
        int scored = 0;
        for (JsonNode match : fixture) {
            assertThat(match.get("date").asText()).matches("\\d{4}-\\d{2}-\\d{2}T18:00:00[+-]\\d{4}");
            if (match.has("homeTeamScore")) {
                scored++;
                assertThat(match.get("homeTeamScore").asInt()).isBetween(0, 4);
                assertThat(match.get("awayTeamScore").asInt()).isBetween(0, 4);
            }
        }
        assertThat(scored).isEqualTo(190);
    }

    @Test
    void positionsAreOrderedByPointsAndConsistent() {
        JsonNode positions = rest.getForObject("/positions", JsonNode.class).get("positions");
        assertThat(positions).hasSize(20);
        int previousPoints = Integer.MAX_VALUE;
        for (int i = 0; i < positions.size(); i++) {
            JsonNode p = positions.get(i);
            assertThat(p.get("position").asInt()).isEqualTo(i + 1);
            int points = p.get("points").asInt();
            assertThat(points).isLessThanOrEqualTo(previousPoints)
                    .isEqualTo(3 * p.get("matchesWon").asInt() + p.get("matchesDraw").asInt());
            assertThat(p.get("matchesWon").asInt() + p.get("matchesDraw").asInt() + p.get("matchesLost").asInt())
                    .isEqualTo(p.get("matchesPlayed").asInt()).isEqualTo(19);
            previousPoints = points;
        }
    }
}
