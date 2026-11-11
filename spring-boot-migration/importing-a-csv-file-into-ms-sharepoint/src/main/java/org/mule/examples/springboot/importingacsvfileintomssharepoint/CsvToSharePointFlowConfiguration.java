package org.mule.examples.springboot.importingacsvfileintomssharepoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Flow {@code importing-a-csv-file-into-ms-sharepointFlow}: {@code file:inbound-endpoint} (poll every 10 s, move the
 * file to the output directory) -> {@code set-variable fileContent} -> {@code sharepoint:folder-create} ->
 * {@code sharepoint:file-add overwrite="true"} -> logger.
 */
@Configuration(proxyBeanMethods = false)
class CsvToSharePointFlowConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CsvToSharePointFlowConfiguration.class);

    @Bean
    IntegrationFlow importCsvIntoSharePointFlow(@Value("${file.input-dir}") File inputDir,
                                                @Value("${file.move-to-dir}") Path moveToDir,
                                                @Value("${file.polling-frequency}") long pollingFrequency,
                                                @Value("${csvImportFolder}") String csvImportFolder,
                                                SharePointClient sharePoint) {
        String folderUrl = "/Shared Documents/" + csvImportFolder;
        return IntegrationFlow
                .from(Files.inboundAdapter(inputDir).autoCreateDirectory(true).preventDuplicates(false).ignoreHidden(true),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(pollingFrequency))))
                .handle(File.class, (file, headers) -> {
                    // Mule's file receiver moves the file to moveToDirectory as it reads it, before the flow runs.
                    byte[] fileContent = readAndMove(file.toPath(), moveToDir);
                    sharePoint.folderCreate(folderUrl);
                    sharePoint.fileAdd(folderUrl + "/contacts.csv", fileContent, true);
                    log.info("Input file stored in the specified folder under the 'Shared Documents' folder");
                    return null;
                })
                .get();
    }

    private static byte[] readAndMove(Path file, Path moveToDir) {
        try {
            byte[] content = java.nio.file.Files.readAllBytes(file);
            java.nio.file.Files.createDirectories(moveToDir);
            java.nio.file.Files.move(file, moveToDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
