package org.mule.examples.springboot.netsuitedataretrieval;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One record returned by {@code netsuite:search}. Mule serialises the connector's record beans as-is
 * ({@code payload map $}), so every field is kept, in order.
 */
public final class NetSuiteRecord {

    private final Map<String, JsonNode> fields = new LinkedHashMap<>();

    @JsonAnySetter
    void set(String name, JsonNode value) {
        fields.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, JsonNode> fields() {
        return fields;
    }
}
