package org.mule.examples.springboot.importleadsintosalesforce;

import org.mule.examples.springboot.importleadsintosalesforce.salesforce.SalesforceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Mule {@code batch:job name="CreateLeadsBatch" max-failed-records="1000"}:
 * <ul>
 *   <li>input: the polled CSV mapped to Lead maps ({@link LeadCsv});</li>
 *   <li>{@code LeadExistsStep}: {@code SELECT Id FROM Lead WHERE Email = '...'} → {@link #leadExistsStep};</li>
 *   <li>{@code LeadInsertStep} ({@code accept-expression="#[!recordVars['exists']]"}, {@code batch:commit size="200"},
 *       {@code sfdc:create type="Lead"}) → chunk writer;</li>
 *   <li>{@code LogFailuresStep} ({@code ONLY_FAILURES}) → skip listener;</li>
 *   <li>{@code on-complete} → job listener.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class CreateLeadsBatchConfig {

    static final String JOB_NAME = "CreateLeadsBatch";
    static final int COMMIT_SIZE = 200;
    static final int MAX_FAILED_RECORDS = 1000;
    private static final Logger log = LoggerFactory.getLogger(CreateLeadsBatchConfig.class);

    @Bean
    Job createLeadsBatch(JobRepository jobRepository, Step createLeadsStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(createLeadsStep)
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution job) {
                        long loaded = job.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
                        long failed = job.getStepExecutions().stream().mapToLong(StepExecution::getSkipCount).sum();
                        log.info("{} Loaded Records {} Failed Records", loaded, failed);
                    }
                })
                .build();
    }

    @Bean
    Step createLeadsStep(JobRepository jobRepository, PlatformTransactionManager tx, ListItemReader<Map<String, Object>> leadsReader,
                         SalesforceClient salesforce) {
        return new StepBuilder("LeadInsertStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(COMMIT_SIZE, tx)
                .reader(leadsReader)
                .processor(leadExistsStep(salesforce))
                .writer(insertLeads(salesforce))
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(MAX_FAILED_RECORDS)
                .noRollback(Exception.class)
                .listener(new SkipListener<Map<String, Object>, Map<String, Object>>() {
                    @Override
                    public void onSkipInProcess(Map<String, Object> item, Throwable t) {
                        log.info("Got Failure {}", item);
                    }

                    @Override
                    public void onSkipInWrite(Map<String, Object> item, Throwable t) {
                        log.info("Got Failure {}", item);
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    ListItemReader<Map<String, Object>> leadsReader(@Value("#{jobParameters['file']}") String file) {
        try {
            return new ListItemReader<>(LeadCsv.toLeads(Files.readString(Path.of(file), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** {@code LeadExistsStep} + the {@code LeadInsertStep} accept-expression: existing leads are filtered out. */
    static ItemProcessor<Map<String, Object>, Map<String, Object>> leadExistsStep(SalesforceClient salesforce) {
        return lead -> {
            boolean exists = !salesforce.query("SELECT Id FROM Lead WHERE Email = '" + lead.get("Email") + "'").isEmpty();
            if (exists) {
                return null;
            }
            log.info("Got Record {}, it exists {}", lead, exists);
            return lead;
        };
    }

    /** {@code batch:commit size="200"} + {@code sfdc:create type="Lead"}. */
    static ItemWriter<Map<String, Object>> insertLeads(SalesforceClient salesforce) {
        return chunk -> salesforce.create("Lead", List.copyOf(chunk.getItems()));
    }

}
