package org.mule.examples.springboot.queryingadbandattachingresultstoanemail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The "Map Collection to CSV" DataWeave script ({@code %output application/csv header=true}): one line per
 * aggregated query result, built from its first row ({@code $[0]}), with {@code dob}/{@code hire_date} as dates.
 */
final class EmployeesCsv {

    static final List<String> COLUMNS = List.of("first_name", "last_name", "gender", "dob", "hire_date");

    private EmployeesCsv() {
    }

    static String write(List<List<Map<String, Object>>> results) {
        StringBuilder csv = new StringBuilder(String.join(",", COLUMNS)).append('\n');
        for (List<Map<String, Object>> result : results) {
            Map<String, Object> first = result.isEmpty() ? Map.of() : result.get(0);
            csv.append(COLUMNS.stream().map(c -> cell(first.get(c))).collect(Collectors.joining(","))).append('\n');
        }
        return csv.toString();
    }

    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        boolean quote = Stream.of(",", "\"", "\n", "\r").anyMatch(text::contains);
        return quote ? '"' + text.replace("\"", "\\\"") + '"' : text;
    }
}
