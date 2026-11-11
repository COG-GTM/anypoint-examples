package org.mule.examples.springboot.importingacsvfileintomongodb;

import com.mongodb.MongoCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** {@code file:inbound-endpoint "Poll files"} as a Spring Integration file inbound adapter feeding {@link CustomerImporter}. */
@Configuration(proxyBeanMethods = false)
class CsvToMongoFlowConfiguration {

    @Bean
    IntegrationFlow csvToMongodbFlow(@Value("${file.input-dir}") File inputDir,
                                     @Value("${file.polling-frequency}") long pollingFrequency,
                                     CustomerImporter importer) {
        return IntegrationFlow
                .from(Files.inboundAdapter(inputDir)
                                .autoCreateDirectory(true)
                                // Mule deletes each file after reading it, so a later file with the same name is read again.
                                .preventDuplicates(false)
                                .ignoreHidden(true),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(pollingFrequency))))
                .handle(File.class, (file, headers) -> {
                    importer.importCsv(read(file));
                    // Mule 3 file connector default autoDelete="true".
                    file.delete();
                    return null;
                })
                .get();
    }

    /** {@code mongo:config username/password}: authenticate against {@code database.name} when a user is configured. */
    @Bean
    MongoClientSettingsBuilderCustomizer muleMongoCredentials(@Value("${database.user:}") String user,
                                                              @Value("${database.password:}") String password,
                                                              @Value("${database.name}") String database) {
        return settings -> {
            if (!user.isBlank()) {
                settings.credential(MongoCredential.createCredential(user, database, password.toCharArray()));
            }
        };
    }

    private static String read(File file) {
        try {
            return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
