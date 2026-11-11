package org.mule.examples.springboot.importcontactsintosalesforce;

import org.mule.examples.springboot.importcontactsintosalesforce.salesforce.SalesforceClient;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flow {@code Contacts_to_SFDC}: {@code file:inbound-endpoint} (poll {@code file.input-path} every
 * {@code file.polling-frequency} ms, move to {@code file.output-path}) → DataWeave "Map CSV to Contacts"
 * → {@code sfdc:create type="Contact"}.
 */
@Configuration(proxyBeanMethods = false)
class ContactsToSfdcFlow {

    private static final Logger log = LoggerFactory.getLogger(ContactsToSfdcFlow.class);
    private static final CsvMapper CSV = new CsvMapper();

    @Bean
    IntegrationFlow contactsToSfdc(SalesforceClient salesforce,
                                   @Value("${file.input-path}") File input,
                                   @Value("${file.output-path}") File output,
                                   @Value("${file.polling-frequency}") long pollingFrequency) {
        return IntegrationFlow
                .from(Files.inboundAdapter(input).autoCreateDirectory(true).preventDuplicates(false),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(pollingFrequency))))
                .handle(File.class, (file, headers) -> {
                    String csv = readAndMove(file, output);
                    List<Map<String, Object>> contacts = toContacts(csv);
                    List<SalesforceClient.SaveResult> results = salesforce.create("Contact", contacts);
                    log.info("Created {} Salesforce Contact(s) from {}", results.stream().filter(SalesforceClient.SaveResult::success).count(), file.getName());
                    return null;
                })
                .get();
    }

    /** DataWeave: {@code payload map { FirstName: $.firstname, LastName: $.surname, Email: $.email, Phone: $.phone }}. */
    static List<Map<String, Object>> toContacts(String csv) {
        List<Map<String, Object>> contacts = new ArrayList<>();
        for (Map<String, String> row : rows(csv)) {
            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("FirstName", row.get("firstname"));
            contact.put("LastName", row.get("surname"));
            contact.put("Email", row.get("email"));
            contact.put("Phone", row.get("phone"));
            contacts.add(contact);
        }
        return contacts;
    }

    static List<Map<String, String>> rows(String csv) {
        try (MappingIterator<Map<String, String>> it = CSV.readerForMapOf(String.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(csv)) {
            return it.readAll();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads the file, then moves it to the output directory like the Mule {@code moveToDirectory}. */
    static String readAndMove(File file, File outputDir) {
        try {
            String content = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            java.nio.file.Files.createDirectories(outputDir.toPath());
            java.nio.file.Files.move(file.toPath(), Path.of(outputDir.getPath(), file.getName()), StandardCopyOption.REPLACE_EXISTING);
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
