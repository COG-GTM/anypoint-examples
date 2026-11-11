package org.mule.examples.springboot.legacymodernization;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mule.examples.springboot.legacymodernization.model.Address;
import org.mule.examples.springboot.legacymodernization.model.OrderItem;
import org.mule.examples.springboot.legacymodernization.model.ShippingOrder;
import org.mule.examples.springboot.legacymodernization.model.ShippingOrderConfirmation;

/** The flow's DataWeave {@code %output application/csv header=true} mapping, one record per order item. */
public final class LegacyFulfillmentCsv {

    static final List<String> HEADER = List.of("MSKU", "QTY",
            "BillingAddressName", "BillingAddressStreet", "BillingAddrCity", "BillingAddrState", "BillingAddrCountry",
            "BillingAddrZipCode", "ShippingAddrName", "ShippingAddrStreet", "ShippingAddrCity", "ShippingAddrState",
            "ShippingAddrCountry", "ShippingAddrZipCode", "ShippingId");

    private LegacyFulfillmentCsv() {
    }

    public static String render(ShippingOrderConfirmation confirmation) {
        ShippingOrder order = confirmation.shippingOrder;
        StringBuilder csv = new StringBuilder(line(HEADER));
        List<OrderItem> items = order.order == null || order.order.orderItemList == null
                ? List.of() : order.order.orderItemList;
        for (OrderItem item : items) {
            csv.append(line(Stream.of(
                    Arrays.asList(item.merchantSKU, String.valueOf(item.quantity)),
                    address(order.billingAddress),
                    address(order.shippingAddress),
                    Arrays.asList(order.shippingId)).flatMap(List::stream).toList()));
        }
        return csv.toString();
    }

    private static List<String> address(Address a) {
        if (a == null) {
            return Arrays.asList(new String[6]);
        }
        String street = a.line1 == null && a.line2 == null
                ? null : Objects.toString(a.line1, "") + " " + Objects.toString(a.line2, "");
        return Arrays.asList(a.name, street, a.city, a.stateOrProvinceCode, a.countryCode, a.postalCode);
    }

    private static String line(List<String> values) {
        return values.stream().map(LegacyFulfillmentCsv::escape).collect(Collectors.joining(",")) + "\n";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}
