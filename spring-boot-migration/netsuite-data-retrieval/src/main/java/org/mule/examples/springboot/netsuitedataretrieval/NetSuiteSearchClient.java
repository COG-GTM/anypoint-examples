package org.mule.examples.springboot.netsuitedataretrieval;

import java.util.List;
import org.mule.examples.springboot.netsuitedataretrieval.NetSuiteSearch.CustomerSearch;
import org.mule.examples.springboot.netsuitedataretrieval.NetSuiteSearch.ItemSupplyPlanSearchBasic;
import org.mule.examples.springboot.netsuitedataretrieval.NetSuiteSearch.OpportunitySearch;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Typed replacement for the Mule {@code netsuite:search} operation. The search criteria object is POSTed as JSON to
 * {@code /search/{searchRecord}} (searchRecord as named in the Mule connector) and the records come back as a JSON array.
 */
@Component
public class NetSuiteSearchClient {

    private static final ParameterizedTypeReference<List<NetSuiteRecord>> RECORDS = new ParameterizedTypeReference<>() {
    };

    private final WebClient webClient;

    public NetSuiteSearchClient(WebClient.Builder builder, NetSuiteProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, nlAuth(properties))
                .build();
    }

    public List<NetSuiteRecord> searchCustomers(CustomerSearch search) {
        return search("CUSTOMER", search);
    }

    public List<NetSuiteRecord> searchItemSupplyPlans(ItemSupplyPlanSearchBasic search) {
        return search("ITEM_SUPPLY_PLAN_BASIC", search);
    }

    public List<NetSuiteRecord> searchOpportunities(OpportunitySearch search) {
        return search("OPPORTUNITY", search);
    }

    private List<NetSuiteRecord> search(String searchRecord, Object criteria) {
        List<NetSuiteRecord> records = webClient.post()
                .uri("/search/{searchRecord}", searchRecord)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(criteria)
                .retrieve()
                .bodyToMono(RECORDS)
                .block();
        return records == null ? List.of() : records;
    }

    static String nlAuth(NetSuiteProperties p) {
        return "NLAuth nlauth_account=" + p.account() + ", nlauth_email=" + p.email()
                + ", nlauth_signature=" + p.password() + ", nlauth_role=" + p.roleId()
                + ", nlauth_application_id=" + p.applicationId();
    }
}
