package org.mule.examples.springboot.sapdataretrieval;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import java.util.LinkedHashMap;
import java.util.Map;

/** One {@code <row>} of a BAPI table, field name → value, in document order (DataWeave {@code ($$):$}). */
public final class SapRow {

    private final Map<String, String> fields = new LinkedHashMap<>();

    void put(String name, String value) {
        fields.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, String> fields() {
        return fields;
    }
}
