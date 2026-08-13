package org.ordermgmt.fulfillment.config;

import java.time.Clock;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Thread pool backing the migrated {@code <async>} scope, and the clock used for legacy file
 * naming.
 */
@Configuration
public class AsyncConfiguration {

    @Bean
    public Executor legacyFulfillmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("legacy-fulfillment-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
