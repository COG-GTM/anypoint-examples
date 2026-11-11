package org.mule.examples.springboot.legacymodernization;

import java.util.List;
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
        for (OrderItem item : order.order.orderItemList) {
            csv.append(line(Stream.of(
                    List.of(item.merchantSKU, String.valueOf(item.quantity)),
                    address(order.billingAddress),
                    address(order.shippingAddress),
                    List.of(order.shippingId)).flatMap(List::stream).toList()));
        }
        return csv.toString();
    }

    private static List<String> address(Address a) {
        return List.of(a.name, a.line1 + " " + a.line2, a.city, a.stateOrProvinceCode, a.countryCode, a.postalCode);
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
