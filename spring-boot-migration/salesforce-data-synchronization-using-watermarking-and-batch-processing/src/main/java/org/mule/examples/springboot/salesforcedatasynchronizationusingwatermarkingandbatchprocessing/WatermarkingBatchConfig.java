package org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing;

import org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing.salesforce.SalesforceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * Mule {@code batch:job name="watermarkingBatch"}: input = the poll's
 * {@code SELECT Id,Name,LastModifiedDate FROM Contact WHERE LastModifiedDate > #[flowVars['timestamp']]},
 * {@code Batch_Step} = {@code logger "Contact recently modified: #[payload]"}.
 */
@Configuration(proxyBeanMethods = false)
class WatermarkingBatchConfig {

    static final String JOB_NAME = "watermarkingBatch";
    /** Mule batch default block size. */
    static final int BLOCK_SIZE = 100;
    private static final Logger log = LoggerFactory.getLogger(WatermarkingBatchConfig.class);

    @Bean
    Job watermarkingBatch(JobRepository jobRepository, Step batchStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(batchStep).build();
    }

    @Bean
    Step batchStep(JobRepository jobRepository, PlatformTransactionManager tx, SalesforceQueryItemReader recentlyModifiedContacts) {
        return new StepBuilder("Batch_Step", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(BLOCK_SIZE, tx)
                .reader(recentlyModifiedContacts)
                .writer(logEachContact())
                .build();
    }

    @Bean
    @StepScope
    SalesforceQueryItemReader recentlyModifiedContacts(SalesforceClient salesforce, @Value("#{jobParameters['timestamp']}") String timestamp) {
        return new SalesforceQueryItemReader(salesforce,
                "SELECT Id,Name,LastModifiedDate FROM Contact WHERE LastModifiedDate > " + timestamp, "LastModifiedDate");
    }

    static ItemWriter<Map<String, Object>> logEachContact() {
        return chunk -> chunk.forEach(contact -> log.info("Contact recently modified: {}", contact));
    }
}
