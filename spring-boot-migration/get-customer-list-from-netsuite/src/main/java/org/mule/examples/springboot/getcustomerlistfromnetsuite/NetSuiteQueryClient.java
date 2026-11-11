package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Typed replacement for the Mule {@code netsuite:query} operation: the DSQL query is POSTed verbatim to the
 * NetSuite SuiteQL REST endpoint and the result rows are mapped onto {@link NetSuiteCustomer}.
 */
@Component
public class NetSuiteQueryClient {

    static final String SUITEQL_PATH = "/services/rest/query/v1/suiteql";

    private final WebClient webClient;

    public NetSuiteQueryClient(WebClient.Builder builder, NetSuiteProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, nlAuth(properties))
                .build();
    }

    public List<NetSuiteCustomer> query(String query) {
        QueryResponse response = webClient.post()
                .uri(SUITEQL_PATH)
                .header("Prefer", "transient")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new QueryRequest(query))
                .retrieve()
                .bodyToMono(QueryResponse.class)
                .block();
        return response == null || response.items() == null ? List.of() : response.items();
    }

    static String nlAuth(NetSuiteProperties p) {
        return "NLAuth nlauth_account=" + p.account() + ", nlauth_email=" + p.email()
                + ", nlauth_signature=" + p.password() + ", nlauth_role=" + p.roleId();
    }

    record QueryRequest(String q) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QueryResponse(List<NetSuiteCustomer> items) {
    }
}
