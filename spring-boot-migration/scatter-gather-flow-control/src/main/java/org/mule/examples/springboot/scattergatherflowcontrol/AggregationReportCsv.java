package org.mule.examples.springboot.scattergatherflowcontrol;

import java.util.List;
import java.util.Map;

/**
 * The DataWeave transform {@code payload map {Id: $.Id, Name: $.Name, Email: $.Email}} with {@code %output application/csv}
 * (header row, comma separator, {@code \n} line endings, null written as an empty field). Merged contacts carry no
 * {@code Id} key, so the Id column is empty, exactly as in Mule.
 */
public final class AggregationReportCsv {

    private static final List<String> COLUMNS = List.of("Id", "Name", "Email");

    private AggregationReportCsv() {
    }

    public static String write(List<Map<String, String>> contacts) {
        StringBuilder csv = new StringBuilder(String.join(",", COLUMNS)).append('\n');
        for (Map<String, String> contact : contacts) {
            csv.append(String.join(",", COLUMNS.stream().map(c -> field(contact.get(c))).toList())).append('\n');
        }
        return csv.toString();
    }

    private static String field(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}
