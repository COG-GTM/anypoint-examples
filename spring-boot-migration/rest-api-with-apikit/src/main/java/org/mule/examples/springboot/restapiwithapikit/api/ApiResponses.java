package org.mule.examples.springboot.restapiwithapikit.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Response bodies, mirroring {@code org.mule.examples.leagues.response.*} (NON_NULL inclusion, ISO-8601 dates). */
public final class ApiResponses {

    private ApiResponses() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"name", "id", "homeCity", "stadium", "matches"})
    public record TeamResponse(String id, String name, String homeCity, String stadium, Integer matches) {
    }

    public record Teams(List<TeamResponse> teams) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"homeTeam", "awayTeam", "date", "homeTeamScore", "awayTeamScore"})
    public record MatchResponse(String homeTeam, String awayTeam,
                                @JsonSerialize(using = IsoDateSerializer.class) Date date,
                                Integer homeTeamScore, Integer awayTeamScore) {
    }

    public record Fixture(List<MatchResponse> fixture) {
    }

    @JsonPropertyOrder({"position", "team", "points", "matchesPlayed", "matchesWon", "matchesDraw", "matchesLost",
            "goalsInFavor", "goalsAgainst"})
    public record Position(int position, String team, int points, int matchesPlayed, int matchesWon, int matchesDraw,
                           int matchesLost, int goalsInFavor, int goalsAgainst) {
    }

    public record Positions(List<Position> positions) {
    }

    public record Message(String message) {
    }

    /** Same format as {@code org.mule.examples.leagues.serializer.JsonDateSerializer}: JVM default time zone. */
    public static class IsoDateSerializer extends StdSerializer<Date> {

        public IsoDateSerializer() {
            super(Date.class);
        }

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(value));
        }
    }
}
