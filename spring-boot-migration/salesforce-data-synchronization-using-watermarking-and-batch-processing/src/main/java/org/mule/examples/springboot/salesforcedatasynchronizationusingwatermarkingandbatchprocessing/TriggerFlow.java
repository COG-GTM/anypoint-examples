package org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing;

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

/**
 * Flow {@code triggerFlow}: {@code poll} ({@code fixed-frequency-scheduler}) with
 * {@code watermark variable="timestamp" default-expression="YESTERDAY" selector="MAX"
 * selector-expression="#[payload.LastModifiedDate]"} → {@code batch:execute name="watermarkingBatch"}.
 * The watermark advances only when the poll's query succeeded and returned records.
 */
@Component
public class TriggerFlow {

    static final String WATERMARK = "triggerFlow.timestamp";
    private static final Logger log = LoggerFactory.getLogger(TriggerFlow.class);

    private final JobLauncher jobLauncher;
    private final Job watermarkingBatch;
    private final WatermarkStore watermarks;
    private final String defaultExpression;

    TriggerFlow(JobLauncher jobLauncher, Job watermarkingBatch, WatermarkStore watermarks,
                @Value("${poll.watermark.default-expression}") String defaultExpression) {
        this.jobLauncher = jobLauncher;
        this.watermarkingBatch = watermarkingBatch;
        this.watermarks = watermarks;
        this.defaultExpression = defaultExpression;
    }

    public JobExecution poll() throws JobExecutionException {
        String timestamp = watermarks.get(WATERMARK).orElse(defaultExpression);
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
