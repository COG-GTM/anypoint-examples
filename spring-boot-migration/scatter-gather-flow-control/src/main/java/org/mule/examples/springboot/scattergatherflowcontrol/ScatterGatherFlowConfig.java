package org.mule.examples.springboot.scattergatherflowcontrol;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;

/** {@code scatter-gatherFlow}: scatter-gather(sourceA, sourceB) → custom aggregation → DataWeave CSV → outboundFlow. */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class ScatterGatherFlowConfig {

    public static final String INPUT = "scatterGatherFlow.input";

    @Bean
    public IntegrationFlow scatterGatherFlow(AggregationReportMailer mailer) {
        return f -> f
                .scatterGather(
                        s -> s.applySequence(true)
                                .recipientFlow(sf -> sf.transform(p -> ContactSources.sourceA()))
                                .recipientFlow(sf -> sf.transform(p -> ContactSources.sourceB())),
                        g -> g.outputProcessor(group -> new MergedContacts(ContactMergeAggregationStrategy.aggregate(group))))
                .<MergedContacts, String>transform(m -> AggregationReportCsv.write(m.contacts()))
                .<String, String>transform(mailer::send);
    }
}
