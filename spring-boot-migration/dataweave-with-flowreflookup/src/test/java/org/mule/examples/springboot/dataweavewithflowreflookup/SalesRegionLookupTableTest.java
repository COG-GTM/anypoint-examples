package org.mule.examples.springboot.dataweavewithflowreflookup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Table-driven parity for the LookUpSalesRegionFlow Groovy switch and the DataWeave field mapping. */
class SalesRegionLookupTableTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(nullValues = "<null>", value = {
            "CT, North East", "ME, North East", "MA, North East", "NH, North East", "VT, North East", "RI, North East",
            "NY, North East", "NJ, North East", "DE, North East", "DC, North East", "MD, North East",
            "AL, South East", "AR, South East", "FL, South East", "GA, South East", "LA, South East", "SC, South East",
            "NC, South East", "TN, South East", "TX, South East",
            "ID, Mid West", "IL, Mid West", "IA, Mid West", "KS, Mid West", "MT, Mid West", "WY, Mid West",
            "ND, Mid West", "SD, Mid West", "OH, Mid West",
            "AZ, South West", "CO, South West", "OK, South West", "NM, South West", "NV, South West",
            "CA, West Coast", "HI, West Coast", "WA, West Coast", "OR, West Coast", "AK, West Coast",
            "fl, South East", "Ca, West Coast",
            "PA, UNKNOWN", "MI, UNKNOWN", "UT, UNKNOWN", "Florida, UNKNOWN", "'', UNKNOWN", "<null>, UNKNOWN"
    })
    void regionLikeGroovySwitch(String state, String region) {
        Map<String, String> row = new HashMap<>();
        row.put("company_state", state);
        assertThat(SalesRegionLookup.lookup(row)).containsExactly(Map.entry("region", region));
        assertThat(AccountMapper.toAccount(row).get("Region__c")).isEqualTo(region);
        assertThat(AccountMapper.toAccount(row).get("BillingState")).isEqualTo(state == null ? null : state.toUpperCase());
    }
}
