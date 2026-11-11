package org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing.salesforce;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Converts REST API records into the shape the Mule Salesforce connector produced: a
 * {@link HashMap} of field name to value plus a {@code type} entry (the sObject type), with
 * date-times in the SOAP API format ({@code 2014-08-25T13:21:00.000Z}, not REST's {@code +0000}).
 * Using a {@code HashMap} keeps the connector's key order wherever a record is printed.
 */
public final class SalesforceRecords {

    private static final Pattern REST_DATETIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+0000");

    private SalesforceRecords() {
    }

    public static Map<String, Object> toMuleMap(JsonNode record) {
        Map<String, Object> map = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = record.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().equals("attributes")) {
                map.put("type", field.getValue().path("type").asText());
            } else {
                map.put(field.getKey(), value(field.getValue()));
            }
        }
        return map;
    }

    static Object value(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            return node.has("records") ? records(node.path("records")) : toMuleMap(node);
        }
        if (node.isTextual()) {
            String text = node.asText();
            return REST_DATETIME.matcher(text).matches() ? text.substring(0, text.length() - 5) + "Z" : text;
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    static List<Map<String, Object>> records(JsonNode records) {
        List<Map<String, Object>> list = new ArrayList<>();
        records.forEach(r -> list.add(toMuleMap(r)));
        return list;
    }
}
