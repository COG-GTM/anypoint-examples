package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Flow {@code triggerFlow}: {@code poll} ({@code fixed-frequency-scheduler frequency="60"}) with
 * {@code watermark variable="timestamp" selector="MAX" selector-expression="#[payload.LastModifiedDate]"} and the
 * default expression "now minus 10 seconds" ({@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}, UTC) → {@code logger
 * "#[flowVars.timestamp]"} → {@code batch:execute name="salesforce-to-database-Batch"}.
 * The watermark advances only when the poll's query succeeded and returned records.
 */
@Component
public class TriggerFlow {

    static final String WATERMARK = "triggerFlow.timestamp";
    private static final Logger log = LoggerFactory.getLogger(TriggerFlow.class);

    private final JobLauncher jobLauncher;
    static final DateTimeFormatter DEFAULT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final Job watermarkingBatch;
    private final WatermarkStore watermarks;
    private final Clock clock;
    private final Duration defaultLookBack;

    TriggerFlow(JobLauncher jobLauncher, Job salesforceToDatabaseBatch, WatermarkStore watermarks, Clock clock,
                @Value("${poll.watermark.default-look-back}") Duration defaultLookBack) {
        this.jobLauncher = jobLauncher;
        this.watermarkingBatch = salesforceToDatabaseBatch;
        this.watermarks = watermarks;
        this.clock = clock;
        this.defaultLookBack = defaultLookBack;
    }

    public JobExecution poll() throws JobExecutionException {
        String timestamp = watermarks.get(WATERMARK).orElseGet(() -> DEFAULT_FORMAT.format(clock.instant().minus(defaultLookBack)));
        log.info(timestamp);
        JobExecution job = jobLauncher.run(watermarkingBatch, new JobParametersBuilder()
                .addString("timestamp", timestamp)
                .addLong("run.at", System.nanoTime())
                .toJobParameters());
        job.getStepExecutions().stream()
                .map(StepExecution::getExecutionContext)
                .filter(ctx -> ctx.containsKey(SalesforceQueryItemReader.NEXT_WATERMARK))
                .map(ctx -> ctx.getString(SalesforceQueryItemReader.NEXT_WATERMARK))
                .findFirst()
                .ifPresent(next -> watermarks.put(WATERMARK, next));
        if (job.getStatus() != BatchStatus.COMPLETED) {
            log.warn("Poll of {} ended {}: {}", WATERMARK, job.getStatus(), job.getAllFailureExceptions());
        }
        return job;
    }
}
