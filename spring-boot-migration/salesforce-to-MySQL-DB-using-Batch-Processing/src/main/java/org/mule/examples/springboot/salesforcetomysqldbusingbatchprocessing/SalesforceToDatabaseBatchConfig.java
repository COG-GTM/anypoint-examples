package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing.salesforce.SalesforceClient;

import java.util.Map;

/**
 * Mule {@code batch:job name="salesforce-to-database-Batch"}:
 * <ul>
 *   <li>input: the poll's {@code SELECT Email,FirstName,LastModifiedDate,LastName FROM Contact WHERE LastModifiedDate > #[timestamp]};</li>
 *   <li>{@code queryExistingContactInDbStep}: DataWeave field mapping + {@code SELECT ... FROM contact WHERE email=?} → processor;</li>
 *   <li>{@code upsertContactInDbStep}: {@code choice} insert / update → writer;</li>
 *   <li>{@code on-complete}: {@code logger "Batch sf->db has finished"}.</li>
 * </ul>
 * Mule's default {@code max-failed-records} is 0, so (as with a non fault-tolerant step) the first failing record stops the job.
 */
@Configuration(proxyBeanMethods = false)
class SalesforceToDatabaseBatchConfig {

    static final String JOB_NAME = "salesforce-to-database-Batch";
    static final int BLOCK_SIZE = 100;
    private static final Logger log = LoggerFactory.getLogger(SalesforceToDatabaseBatchConfig.class);

    @Bean
    Job salesforceToDatabaseBatch(JobRepository jobRepository, Step salesforceToDatabaseStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(salesforceToDatabaseStep)
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution job) {
                        log.info("Batch sf->db has finished");
                    }
                })
                .build();
    }

    @Bean
    Step salesforceToDatabaseStep(JobRepository jobRepository, PlatformTransactionManager tx,
                                  SalesforceQueryItemReader modifiedContacts, JdbcTemplate jdbc) {
        return new StepBuilder("upsertContactInDbStep", jobRepository)
                .<Map<String, Object>, DbContact>chunk(BLOCK_SIZE, tx)
                .reader(modifiedContacts)
                .processor(queryExistingContactInDb(jdbc))
                .writer(upsertContactInDb(jdbc))
                .build();
    }

    @Bean
    @StepScope
    SalesforceQueryItemReader modifiedContacts(SalesforceClient salesforce, @Value("#{jobParameters['timestamp']}") String timestamp) {
        return new SalesforceQueryItemReader(salesforce,
                "SELECT Email,FirstName,LastModifiedDate,LastName FROM Contact WHERE LastModifiedDate > " + timestamp, "LastModifiedDate");
    }

    /** {@code queryExistingContactInDbStep}. */
    static ItemProcessor<Map<String, Object>, DbContact> queryExistingContactInDb(JdbcTemplate jdbc) {
        return sfdc -> {
            String email = (String) sfdc.get("Email");
            boolean exists = !jdbc.queryForList("SELECT first_name,last_name,email FROM contact WHERE email=?", email).isEmpty();
            return new DbContact(email, (String) sfdc.get("FirstName"), (String) sfdc.get("LastName"),
                    (String) sfdc.get("LastModifiedDate"), exists);
        };
    }

    /** {@code upsertContactInDbStep}: {@code when exists==false} insert, {@code when exists==true} update. */
    static ItemWriter<DbContact> upsertContactInDb(JdbcTemplate jdbc) {
        return chunk -> {
            for (DbContact c : chunk) {
                if (!c.exists()) {
                    jdbc.update("INSERT INTO contact (first_name, last_name, email) VALUES (?,?,?)", c.firstName(), c.lastName(), c.email());
                } else {
                    jdbc.update("UPDATE contact SET first_name=?,last_name=? WHERE email = ?", c.firstName(), c.lastName(), c.email());
                }
            }
        };
    }
}
