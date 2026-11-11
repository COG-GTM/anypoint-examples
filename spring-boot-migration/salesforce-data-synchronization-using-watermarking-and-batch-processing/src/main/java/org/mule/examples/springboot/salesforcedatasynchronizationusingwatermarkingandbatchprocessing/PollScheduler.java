package org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing;

import org.springframework.batch.core.JobExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** {@code fixed-frequency-scheduler frequency="10" timeUnit="SECONDS"}, starting right away. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "poll.enabled", havingValue = "true", matchIfMissing = true)
class PollScheduler {

    private final TriggerFlow triggerFlow;

    PollScheduler(TriggerFlow triggerFlow) {
        this.triggerFlow = triggerFlow;
    }

    @Scheduled(fixedRateString = "${poll.frequency}", initialDelay = 0)
    void poll() throws JobExecutionException {
        triggerFlow.poll();
    }
}
