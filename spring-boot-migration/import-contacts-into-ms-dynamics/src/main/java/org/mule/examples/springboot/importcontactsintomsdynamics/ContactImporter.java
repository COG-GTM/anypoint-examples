package org.mule.examples.springboot.importcontactsintomsdynamics;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Body of the Mule flow {@code crm-create-account} after the file has been picked up. */
@Component
public class ContactImporter {

    private static final Logger log = LoggerFactory.getLogger(ContactImporter.class);

    private final CsvMapper csvMapper = new CsvMapper();
    private final DynamicsCrmClient dynamics;
    private final Path outputDir;

    public ContactImporter(DynamicsCrmClient dynamics, ContactImportProperties properties) {
        this.dynamics = dynamics;
        this.outputDir = Path.of(properties.outputDir());
    }

    public void importFile(Path file) {
        List<ContactRecord> contacts = read(file).stream().map(ContactRecord::from).toList();
        moveToOutput(file);
        for (ContactRecord contact : contacts) {
            String id = dynamics.createContact(DynamicsContact.from(contact));
            log.info("Created contact {} {} ({})", contact.firstname(), contact.lastname(), id);
        }
    }

    List<CsvContact> read(Path file) {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        try (MappingIterator<CsvContact> rows = csvMapper.readerFor(CsvContact.class).with(schema)
                .readValues(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            return rows.readAll();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void moveToOutput(Path file) {
        try {
            Files.createDirectories(outputDir);
            Files.move(file, outputDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
