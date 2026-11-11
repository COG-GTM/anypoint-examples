package org.mule.examples.springboot.additionusingjavascripttransformer;

import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway(defaultRequestChannel = AdditionFlowConfig.INPUT)
public interface CalculatorGateway {

    String calculate(String jsonPayload);
}
