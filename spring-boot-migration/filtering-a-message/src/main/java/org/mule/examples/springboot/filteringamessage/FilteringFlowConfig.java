package org.mule.examples.springboot.filteringamessage;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;

/** Spring Integration equivalent of {@code filteringFlow1}. */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class FilteringFlowConfig {

    public static final String INPUT = "filteringFlow.input";
    public static final String GRANTED = "the discount was granted.";
    /** A filtered Mule message ends the flow; the HTTP listener then answers with an empty body. */
    public static final String FILTERED = "";

    private static final Logger log = LoggerFactory.getLogger(FilteringFlowConfig.class);

    @Bean
    public IntegrationFlow filteringFlow() {
        return f -> f
                .<Map<String, Object>>filter(FreeMembershipDiscountFilter::accept,
                        e -> e.discardFlow(d -> d.transform(p -> FILTERED)))
                .transform(p -> GRANTED)
                .handle((p, h) -> {
                    log.info("{}", p);
                    return p;
                });
    }
}
