package org.ordermgmt.fulfillment.service;

import java.util.List;
import org.ordermgmt.fulfillment.model.Address;
import org.ordermgmt.fulfillment.model.OrderItem;
import org.ordermgmt.fulfillment.model.ShippingOrder;
import org.springframework.stereotype.Component;

/**
 * Java re-implementation of the {@code dw:transform-message} step
 * "Map Fullfilment order to CSV". One CSV row is emitted per order item, with the
 * shipping-order level fields repeated on every row.
 *
 * <p>Original DataWeave 1.0 script:
 * <pre>{@code
 * %dw 1.0
 * %output application/csv header=true
 * ---
 * payload.shippingOrder.order.orderItemList map
 * {
 *     MSKU                   : $.merchantSKU,
 *     QTY                    : $.quantity,
 *     BillingAddressName     : payload.shippingOrder.billingAddress.name,
 *     BillingAddressStreet   : payload.shippingOrder.billingAddress.line1 ++ ' ' ++ payload.shippingOrder.billingAddress.line2,
 *     BillingAddrCity        : payload.shippingOrder.billingAddress.city,
 *     BillingAddrState       : payload.shippingOrder.billingAddress.stateOrProvinceCode,
 *     BillingAddrCountry     : payload.shippingOrder.billingAddress.countryCode,
 *     BillingAddrZipCode     : payload.shippingOrder.billingAddress.postalCode,
 *     ShippingAddrName       : payload.shippingOrder.shippingAddress.name,
 *     ShippingAddrStreet     : payload.shippingOrder.shippingAddress.line1 ++ ' ' ++ payload.shippingOrder.shippingAddress.line2,
 *     ShippingAddrCity       : payload.shippingOrder.shippingAddress.city,
 *     ShippingAddrState      : payload.shippingOrder.shippingAddress.stateOrProvinceCode,
 *     ShippingAddrCountry    : payload.shippingOrder.shippingAddress.countryCode,
 *     ShippingAddrZipCode    : payload.shippingOrder.shippingAddress.postalCode,
 *     ShippingId             : payload.shippingOrder.shippingId
 * }
 * }</pre>
 */
@Component
public class ShippingOrderCsvTransformer {

    static final List<String> HEADERS = List.of(
            "MSKU",
            "QTY",
            "BillingAddressName",
            "BillingAddressStreet",
            "BillingAddrCity",
            "BillingAddrState",
            "BillingAddrCountry",
            "BillingAddrZipCode",
            "ShippingAddrName",
            "ShippingAddrStreet",
            "ShippingAddrCity",
            "ShippingAddrState",
            "ShippingAddrCountry",
            "ShippingAddrZipCode",
            "ShippingId");

    private static final String SEPARATOR = ",";
    private static final String LINE_ENDING = "\n";

    public String toCsv(ShippingOrder shippingOrder) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(SEPARATOR, HEADERS)).append(LINE_ENDING);
        if (shippingOrder == null || shippingOrder.order() == null) {
            return csv.toString();
        }
        Address billing = shippingOrder.billingAddress();
        Address shipping = shippingOrder.shippingAddress();
        for (OrderItem item : shippingOrder.order().orderItemList()) {
            csv.append(String.join(SEPARATOR, List.of(
                            quote(item.merchantSKU()),
                            quote(Integer.toString(item.quantity())),
                            quote(name(billing)),
                            quote(street(billing)),
                            quote(city(billing)),
                            quote(state(billing)),
                            quote(country(billing)),
                            quote(postalCode(billing)),
                            quote(name(shipping)),
                            quote(street(shipping)),
                            quote(city(shipping)),
                            quote(state(shipping)),
                            quote(country(shipping)),
                            quote(postalCode(shipping)),
                            quote(shippingOrder.shippingId()))))
                    .append(LINE_ENDING);
        }
        return csv.toString();
    }

    /**
     * Reproduces {@code line1 ++ ' ' ++ line2}. DataWeave 1.0 propagates null through {@code ++},
     * so a missing line yields an empty street rather than the literal text "null".
     */
    private String street(Address address) {
        if (address == null || address.line1() == null || address.line2() == null) {
            return "";
        }
        return address.line1() + " " + address.line2();
    }

    private String name(Address address) {
        return address == null ? "" : nullToEmpty(address.name());
    }

    private String city(Address address) {
        return address == null ? "" : nullToEmpty(address.city());
    }

    private String state(Address address) {
        return address == null ? "" : nullToEmpty(address.stateOrProvinceCode());
    }

    private String country(Address address) {
        return address == null ? "" : nullToEmpty(address.countryCode());
    }

    private String postalCode(Address address) {
        return address == null ? "" : nullToEmpty(address.postalCode());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String quote(String value) {
        String text = nullToEmpty(value);
        if (text.contains(SEPARATOR) || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
