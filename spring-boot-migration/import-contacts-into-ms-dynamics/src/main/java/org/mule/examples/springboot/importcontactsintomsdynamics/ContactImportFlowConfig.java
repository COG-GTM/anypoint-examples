package org.mule.examples.springboot.importcontactsintomsdynamics;

import java.io.File;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;

/** Mule flow {@code crm-create-account}: {@code file:inbound-endpoint} polling the input directory. */
@Configuration
public class ContactImportFlowConfig {

    @Bean
    IntegrationFlow crmCreateAccount(ContactImportProperties properties, ContactImporter importer) {
        return IntegrationFlow
                .from(Files.inboundAdapter(new File(properties.inputDir())).autoCreateDirectory(true),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(properties.pollingFrequency()))))
                .handle(File.class, (file, headers) -> {
                    importer.importFile(file.toPath());
                    return null;
                })
                .get();
    }
}
