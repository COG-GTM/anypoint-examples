package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Typed clients replacing the {@code cxf:jaxws-client} + {@code http:request} chains to the credit agency
 * ({@code /mule/TheCreditAgencyService}) and the banks ({@code /mule/TheBankN}). JSON over HTTP instead of SOAP.
 */
@Component
public class LoanBrokerClients {

    private final RestClient rest;

    public LoanBrokerClients(RestClient.Builder builder, @Value("${loanbroker.services-base-url}") String baseUrl) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    public Model.CreditProfile creditProfile(Model.Customer customer) {
        return rest.post().uri("/mule/TheCreditAgencyService").contentType(MediaType.APPLICATION_JSON)
                .body(customer).retrieve().body(Model.CreditProfile.class);
    }

    public Model.LoanQuote loanQuote(String bank, Model.CreditProfile profile) {
        return rest.post().uri("/mule/{bank}", bank).contentType(MediaType.APPLICATION_JSON)
                .body(new Model.LoanBrokerQuoteRequest(profile)).retrieve().body(Model.LoanQuote.class);
    }
}
