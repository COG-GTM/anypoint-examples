package org.mule.examples.springboot.importingacsvfileintomongodb;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "CSV to Map" DataWeave script ({@code payload map { firstname: $.firstname, surname: $.surname,
 * phone: $.phone, email: $.email }}). DataWeave reads CSV with a header row, comma separator and double quotes; all
 * values stay strings, and a missing column becomes {@code null}.
 */
final class CustomerCsv {

    static final List<String> FIELDS = List.of("firstname", "surname", "phone", "email");

    private CustomerCsv() {
    }

    static List<Map<String, Object>> parse(String csv) {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            List<Map<String, Object>> customers = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, Object> customer = new LinkedHashMap<>();
                for (String field : FIELDS) {
                    customer.put(field, record.isMapped(field) && record.isSet(field) ? record.get(field) : null);
                }
                customers.add(customer);
            }
            return customers;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
