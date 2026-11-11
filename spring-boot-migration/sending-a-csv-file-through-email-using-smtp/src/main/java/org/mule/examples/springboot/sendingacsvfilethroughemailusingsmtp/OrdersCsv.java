package org.mule.examples.springboot.sendingacsvfilethroughemailusingsmtp;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "Transform CSV to Map" DataWeave script ({@code %output application/java}):
 * <pre>
 * payload map { name: $.name, orderId: $.orderId, pricePerUnit: $.pricePerUnit as :number,
 *               units: $.units as :number, totalPrice: ($.units * $.pricePerUnit) as :number }
 * </pre>
 * DataWeave hands integral numbers to Java as integers ({@code "2.0" -> 2}, {@code 2.0 * 10 -> 20}) and other numbers
 * as decimals, so {@link List#toString()} gives the email body documented in {@code src/main/resources/email.txt}.
 */
final class OrdersCsv {

    private OrdersCsv() {
    }

    static List<Map<String, Object>> parse(String csv) {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            List<Map<String, Object>> orders = new ArrayList<>();
            for (CSVRecord record : parser) {
                BigDecimal units = new BigDecimal(record.get("units").trim());
                BigDecimal pricePerUnit = new BigDecimal(record.get("pricePerUnit").trim());
                Map<String, Object> order = new LinkedHashMap<>();
                order.put("name", record.get("name"));
                order.put("orderId", record.get("orderId"));
                order.put("pricePerUnit", javaNumber(pricePerUnit));
                order.put("units", javaNumber(units));
                order.put("totalPrice", javaNumber(units.multiply(pricePerUnit)));
                orders.add(order);
            }
            return orders;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Number javaNumber(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            long asLong = stripped.longValueExact();
            return asLong >= Integer.MIN_VALUE && asLong <= Integer.MAX_VALUE ? (Number) (int) asLong : (Number) asLong;
        }
        return new BigDecimal(stripped.toPlainString());
    }
}
