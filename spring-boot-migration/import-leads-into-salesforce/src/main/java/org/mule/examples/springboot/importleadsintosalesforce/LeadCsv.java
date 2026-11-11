package org.mule.examples.springboot.importleadsintosalesforce;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** DataWeave "Transform CSV to Maps": {@code payload map { Company, Email, FirstName, LastName }}. */
final class LeadCsv {

    private static final CsvMapper CSV = new CsvMapper();

    private LeadCsv() {
    }

    static List<Map<String, Object>> toLeads(String csv) {
        List<Map<String, Object>> leads = new ArrayList<>();
        try (MappingIterator<Map<String, String>> rows = CSV.readerForMapOf(String.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(csv)) {
            for (Map<String, String> row : rows.readAll()) {
                Map<String, Object> lead = new LinkedHashMap<>();
                lead.put("Company", row.get("Company"));
                lead.put("Email", row.get("Email"));
                lead.put("FirstName", row.get("FirstName"));
                lead.put("LastName", row.get("LastName"));
                leads.add(lead);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return leads;
    }
}
