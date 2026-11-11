package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;

/**
 * {@code LoanBrokerFlow}: enrich with credit profile → enrich with banks (choice) → foreach bank: lookupLoanQuote →
 * findLowestLoanQuote. The foreach is a splitter over the routed banks and an aggregator collecting the quotes in
 * bank order.
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class LoanBrokerFlowConfig {

    public static final String INPUT = "loanBrokerFlow.input";
    static final String CREDIT_PROFILE = "creditProfile";
    static final String BANKS = "banks";

    @Bean
    public IntegrationFlow loanBrokerFlow(LoanBrokerClients clients) {
        return f -> f
                .<Model.CustomerQuoteRequest>enrichHeaders(h -> h
                        .headerFunction(CREDIT_PROFILE,
                                (Message<Model.CustomerQuoteRequest> m) -> clients.creditProfile(m.getPayload().customer()))
                        .headerFunction(BANKS,
                                (Message<Model.CustomerQuoteRequest> m) -> BankRouting.banksFor(m.getPayload().loanAmount())))
                .split(Message.class, m -> m.getHeaders().get(BANKS, List.class))
                .<String>handle((bank, h) -> clients.loanQuote(bank, h.get(CREDIT_PROFILE, Model.CreditProfile.class)))
                .aggregate()
                .<List<Model.LoanQuote>, String>transform(quotes -> String.valueOf(BankRouting.lowest(quotes)));
    }
}
