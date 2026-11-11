package org.mule.examples.springboot.dataweavewithflowreflookup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The "CSV to Maps" DataWeave transform: one Salesforce Account map per CSV row. */
public final class AccountMapper {

    private AccountMapper() {
    }

    public static List<Map<String, Object>> toAccounts(List<Map<String, String>> rows) {
        return rows.stream().map(AccountMapper::toAccount).toList();
    }

    static Map<String, Object> toAccount(Map<String, String> row) {
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("Name", row.get("company_name"));
        account.put("BillingStreet", row.get("company_address"));
        account.put("BillingCity", row.get("company_city"));
        String state = row.get("company_state");
        account.put("BillingState", state == null ? null : state.toUpperCase());
        account.put("BillingPostalCode", row.get("company_zip"));
        account.put("Region__c", SalesRegionLookup.lookup(row).get("region"));
        return account;
    }
}
