package org.mule.examples.springboot.restapiwithapikit.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.mule.examples.springboot.restapiwithapikit.api.ApiExceptions.BadRequestException;

/** Hand-written equivalent of the APIkit RAML/JSON-schema (draft-03) validation for leagues.raml. */
final class RamlValidator {

    private RamlValidator() {
    }

    /** uriParameters teamId/homeTeamId/awayTeamId: type string, minLength 3, maxLength 3. */
    static String teamCode(String name, String value) {
        if (value == null || value.length() != 3) {
            throw new BadRequestException("Invalid value '" + value + "' for uri parameter " + name);
        }
        return value;
    }

    static JsonNode object(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new BadRequestException("Body must be a JSON object");
        }
        return body;
    }

    /** Absent → null; present values (including JSON null) must match the schema's {@code type: string}. */
    static String optionalString(JsonNode body, String field) {
        JsonNode v = body.get(field);
        if (v == null) {
            return null;
        }
        if (!v.isTextual()) {
            throw new BadRequestException(field + " must be a string");
        }
        return v.asText();
    }

    /**
     * Absent → 0, the default of the primitive {@code int} fields of {@code org.mule.examples.leagues.request.Score};
     * present values (including JSON null) must match the schema's {@code type: integer}.
     */
    static int integerOrZero(JsonNode body, String field) {
        JsonNode v = body.get(field);
        if (v == null) {
            return 0;
        }
        if (!v.isIntegralNumber()) {
            throw new BadRequestException(field + " must be an integer");
        }
        return v.intValue();
    }

    /** schemas/teams-schema-output.json: id is a 3-character string. */
    static String teamId(JsonNode body) {
        String id = optionalString(body, "id");
        if (id != null && id.length() != 3) {
            throw new BadRequestException("id must have exactly 3 characters");
        }
        return id;
    }
}
