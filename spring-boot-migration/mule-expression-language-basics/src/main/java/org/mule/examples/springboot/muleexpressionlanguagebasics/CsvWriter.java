package org.mule.examples.springboot.muleexpressionlanguagebasics;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Minimal equivalent of DataWeave {@code %output application/csv header=false} for a single record. */
final class CsvWriter {

    private CsvWriter() {
    }

    static String row(String... values) {
        return Arrays.stream(values).map(CsvWriter::quote).collect(Collectors.joining(",")) + "\n";
    }

    private static String quote(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}
