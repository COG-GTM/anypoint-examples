package org.mule.examples.springboot.muleexpressionlanguagebasics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** {@code file:outbound-endpoint path="Path_of_your_choice"}: one new file per message. */
@Component
public class OutputFileWriter {

    private final Path directory;

    public OutputFileWriter(@Value("${mel.output-dir:Path_of_your_choice}") String directory) {
        this.directory = Path.of(directory);
    }

    public Path write(String content) {
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve(UUID.randomUUID() + ".dat");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path directory() {
        return directory;
    }
}
