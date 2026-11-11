package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Typed replacement for the Mule {@code netsuite:query} operation: the DSQL query is POSTed verbatim to the
 * NetSuite SuiteQL REST endpoint and the result rows are mapped onto {@link NetSuiteCustomer}. Like the connector, it
 * returns every matching record: SuiteQL pages ({@code hasMore}) are followed with {@code offset}.
 */
@Component
public class NetSuiteQueryClient {

    static final String SUITEQL_PATH = "/services/rest/query/v1/suiteql";
    static final int PAGE_SIZE = 1000;

    private final WebClient webClient;

    public NetSuiteQueryClient(WebClient.Builder builder, NetSuiteProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, nlAuth(properties))
                .build();
    }

    public List<NetSuiteCustomer> query(String query) {
        List<NetSuiteCustomer> customers = new ArrayList<>();
        int offset = 0;
        while (true) {
            QueryResponse page = page(query, offset);
            if (page == null || page.items() == null || page.items().isEmpty()) {
                return customers;
            }
            customers.addAll(page.items());
            if (!Boolean.TRUE.equals(page.hasMore())) {
                return customers;
            }
            offset += page.items().size();
        }
    }

    private QueryResponse page(String query, int offset) {
        return webClient.post()
                .uri(b -> b.path(SUITEQL_PATH).queryParam("limit", PAGE_SIZE).queryParam("offset", offset).build())
                .header("Prefer", "transient")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new QueryRequest(query))
                .retrieve()
                .bodyToMono(QueryResponse.class)
                .block();
    }

    static String nlAuth(NetSuiteProperties p) {
        return "NLAuth nlauth_account=" + p.account() + ", nlauth_email=" + p.email()
                + ", nlauth_signature=" + p.password() + ", nlauth_role=" + p.roleId();
    }

    record QueryRequest(String q) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QueryResponse(List<NetSuiteCustomer> items, Boolean hasMore) {
    }
}
