package org.mule.examples.springboot.netsuitedataretrieval;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Typed search criteria, mirroring the NetSuite SuiteTalk classes the Mule DataWeave scripts instantiate
 * ({@code CustomerSearch}, {@code ItemSupplyPlanSearchBasic}, {@code OpportunitySearch}).
 */
public final class NetSuiteSearch {

    private NetSuiteSearch() {
    }

    /** {@code com.netsuite.webservices.platform.core.SearchStringField}. */
    public record SearchStringField(String operator, String searchValue) {
        public static SearchStringField startsWith(String value) {
            return value == null ? null : new SearchStringField("STARTS_WITH", value);
        }
    }

    /** {@code com.netsuite.webservices.platform.core.SearchLongField}. */
    public record SearchLongField(String operator, Long searchValue) {
    }

    /** {@code com.netsuite.webservices.lists.relationships.CustomerSearch}. */
    public record CustomerSearch(Basic basic) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Basic(SearchStringField companyName) {
        }

        public static CustomerSearch companyNameStartsWith(String name) {
            return new CustomerSearch(new Basic(SearchStringField.startsWith(name)));
        }
    }

    /** {@code com.netsuite.webservices.platform.common.ItemSupplyPlanSearchBasic}. */
    public record ItemSupplyPlanSearchBasic(SearchLongField quantity) {
    }

    /** {@code com.netsuite.webservices.transactions.sales.OpportunitySearch}. */
    public record OpportunitySearch(Basic basic) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Basic(SearchStringField title) {
        }

        public static OpportunitySearch titleStartsWith(String title) {
            return new OpportunitySearch(new Basic(SearchStringField.startsWith(title)));
        }
    }
}
