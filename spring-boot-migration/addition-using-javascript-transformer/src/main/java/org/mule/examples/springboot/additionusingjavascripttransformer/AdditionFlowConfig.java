package org.mule.examples.springboot.additionusingjavascripttransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Spring Integration equivalent of {@code javascript-calculatorFlow1}. */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class AdditionFlowConfig {

    public static final String INPUT = "javascriptCalculatorFlow.input";

    private static final Logger log = LoggerFactory.getLogger(AdditionFlowConfig.class);

    @Bean
    public IntegrationFlow javascriptCalculatorFlow(ObjectMapper mapper) {
        return f -> f
                .<String, String>transform(body -> {
                    try {
                        return JavaScriptSum.sum(mapper.readTree(body));
                    } catch (java.io.IOException e) {
                        throw new IllegalArgumentException("SyntaxError: " + e.getMessage(), e);
                    }
                })
                .<String, String>transform(sum -> {
                    log.info("Sum is: {}", sum);
                    return "Sum is: " + sum + ".";
                });
    }
}
