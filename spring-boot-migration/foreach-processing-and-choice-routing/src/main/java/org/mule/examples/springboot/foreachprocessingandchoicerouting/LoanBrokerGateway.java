package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway(defaultRequestChannel = LoanBrokerFlowConfig.INPUT)
public interface LoanBrokerGateway {

    String lowestQuote(Model.CustomerQuoteRequest request);
}
