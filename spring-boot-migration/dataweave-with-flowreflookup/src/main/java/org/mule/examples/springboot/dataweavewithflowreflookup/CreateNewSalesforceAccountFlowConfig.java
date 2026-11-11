package org.mule.examples.springboot.dataweavewithflowreflookup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * {@code CreateNewSalesforceAccountFlow}: file:inbound-endpoint (poll {@code input.dir} every
 * {@code polling.frequency} ms, move to {@code output.dir}) → DataWeave CSV-to-Accounts with the region lookup
 * → sfdc:create Account.
 */
@Configuration
@EnableIntegration
public class CreateNewSalesforceAccountFlowConfig {

    private static final Logger log = LoggerFactory.getLogger(CreateNewSalesforceAccountFlowConfig.class);
    private static final CsvMapper CSV = new CsvMapper();

    @Bean
    public IntegrationFlow createNewSalesforceAccountFlow(SalesforceClient salesforce,
                                                          @Value("${input.dir:src/main/resources/input}") String inputDir,
                                                          @Value("${output.dir:src/main/resources/output}") String outputDir,
                                                          @Value("${polling.frequency:10000}") long pollingFrequency) {
        return IntegrationFlow
                .from(org.springframework.integration.file.dsl.Files.inboundAdapter(new File(inputDir))
                                .autoCreateDirectory(true)
                                .preventDuplicates(false),
                        e -> e.poller(Pollers.fixedDelay(pollingFrequency)))
                .<File, List<Map<String, String>>>transform(file -> readAndMove(file, Path.of(outputDir)))
                .<List<Map<String, String>>, List<Map<String, Object>>>transform(AccountMapper::toAccounts)
                .handle((accounts, headers) -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> objects = (List<Map<String, Object>>) accounts;
                    salesforce.create("Account", objects);
                    return null;
                })
                .get();
    }

    /** Reads the CSV (header row) and, like moveToDirectory, moves the file once it has been consumed. */
    static List<Map<String, String>> readAndMove(File file, Path outputDir) {
        try {
            List<Map<String, String>> rows;
            try (MappingIterator<Map<String, String>> it = CSV.readerForMapOf(String.class)
                    .with(CsvSchema.emptySchema().withHeader())
                    .readValues(file)) {
                rows = it.readAll();
            }
            Files.createDirectories(outputDir);
            Files.move(file.toPath(), outputDir.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            log.info("Read {} row(s) from {}", rows.size(), file);
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
