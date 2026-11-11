package org.mule.examples.springboot.importleadsintosalesforce;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Mule {@code batch:input} → {@code file:inbound-endpoint path="src/main/resources/input"
 * moveToDirectory="src/main/resources/output" pollingFrequency="10000"}: each new file is moved to the
 * output folder and the {@code CreateLeadsBatch} job runs over it.
 */
@Configuration(proxyBeanMethods = false)
class LeadsFileTrigger {

    @Bean
    IntegrationFlow pollCsvFiles(JobLauncher jobLauncher, Job createLeadsBatch,
                                 @Value("${file.input-path}") File input,
                                 @Value("${file.output-path}") File output,
                                 @Value("${file.polling-frequency}") long pollingFrequency) {
        return IntegrationFlow
                .from(Files.inboundAdapter(input).autoCreateDirectory(true).preventDuplicates(false),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(pollingFrequency))))
                .handle(File.class, (file, headers) -> {
                    Path moved = move(file, output);
                    run(jobLauncher, createLeadsBatch, moved);
                    return null;
                })
                .get();
    }

    static void run(JobLauncher jobLauncher, Job job, Path file) {
        try {
            jobLauncher.run(job, new JobParametersBuilder()
                    .addString("file", file.toAbsolutePath().toString())
                    .addLong("run.at", System.nanoTime())
                    .toJobParameters());
        } catch (JobExecutionException e) {
            throw new IllegalStateException("Could not start " + job.getName() + " for " + file, e);
        }
    }

    static Path move(File file, File outputDir) {
        try {
            java.nio.file.Files.createDirectories(outputDir.toPath());
            return java.nio.file.Files.move(file.toPath(), Path.of(outputDir.getPath(), file.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
