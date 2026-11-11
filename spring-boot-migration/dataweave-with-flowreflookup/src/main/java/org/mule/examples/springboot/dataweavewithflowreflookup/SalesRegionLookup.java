package org.mule.examples.springboot.dataweavewithflowreflookup;

import java.util.Map;
import java.util.Set;

/** {@code LookUpSalesRegionFlow}: the Groovy state → region switch, invoked from DataWeave via {@code lookup()}. */
public final class SalesRegionLookup {

    public static final String UNKNOWN = "UNKNOWN";

    private static final Map<String, Set<String>> REGIONS = new java.util.LinkedHashMap<>();

    static {
        REGIONS.put("North East", Set.of("CT", "ME", "MA", "NH", "VT", "RI", "NY", "NJ", "DE", "DC", "MD"));
        REGIONS.put("South East", Set.of("AL", "AR", "FL", "GA", "LA", "SC", "NC", "TN", "TX"));
        REGIONS.put("Mid West", Set.of("ID", "IL", "IA", "KS", "MT", "WY", "ND", "SD", "OH"));
        REGIONS.put("South West", Set.of("AZ", "CO", "OK", "NM", "NV"));
        REGIONS.put("West Coast", Set.of("CA", "HI", "WA", "OR", "AK"));
    }

    private SalesRegionLookup() {
    }

    /** @return {@code ["region": region]} for the given CSV row (uses {@code company_state}, case-insensitively). */
    public static Map<String, String> lookup(Map<String, String> row) {
        String state = row.get("company_state");
        if (state != null) {
            state = state.toUpperCase();
        }
        String region = UNKNOWN;
        for (Map.Entry<String, Set<String>> e : REGIONS.entrySet()) {
            if (state != null && e.getValue().contains(state)) {
                region = e.getKey();
                break;
            }
        }
        return Map.of("region", region);
    }
}
