package org.ordermgmt.fulfillment;

import java.util.List;
import org.ordermgmt.fulfillment.model.Address;
import org.ordermgmt.fulfillment.model.Order;
import org.ordermgmt.fulfillment.model.OrderItem;
import org.ordermgmt.fulfillment.model.ShippingOrder;

/**
 * Request and response fixtures copied from the Mule application
 * ({@code legacy-modernization/src/test/resources/message.xml} and {@code reply.xml}).
 */
public final class SoapFixtures {

    public static final String REQUEST = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ord="http://ordermgmt.org/">
            <soapenv:Header/>
            <soapenv:Body>
            <ord:putShippingOrder>
            <shippingId>1234</shippingId>
            <billingAddress>
            <name>Mulesoft</name>
            <line1>77 Geary St</line1>
            <line2>Level 4</line2>
            <city>San Francisco</city>
            <stateOrProvinceCode>CA</stateOrProvinceCode>
            <countryCode>USA</countryCode>
            <postalCode>94108</postalCode>
            </billingAddress>
            <shippingAddress>
            <name>Mulesoft</name>
            <line1>77 Geary St</line1>
            <line2>Level 4</line2>
            <city>San Francisco</city>
            <stateOrProvinceCode>CA</stateOrProvinceCode>
            <countryCode>USA</countryCode>
            <postalCode>94108</postalCode>
            </shippingAddress>
            <order>
            <orderItem>
            <merchantSKU>1234</merchantSKU>
            <quantity>500</quantity>
            </orderItem>
            <orderItem>
            <merchantSKU>6789</merchantSKU>
            <quantity>1500</quantity>
            </orderItem>
            <orderItem>
            <merchantSKU>9998</merchantSKU>
            <quantity>5000</quantity>
            </orderItem>
            </order>
            </ord:putShippingOrder>
            </soapenv:Body>
            </soapenv:Envelope>""";

    public static final String EXPECTED_REPLY =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<ns2:putShippingOrderResponse xmlns:ns2=\"http://ordermgmt.org/\"><ShippingOrderConfirmation>"
            + "<shippingOrder><shippingId>1234</shippingId><billingAddress><name>Mulesoft</name>"
            + "<line1>77 Geary St</line1><line2>Level 4</line2><city>San Francisco</city>"
            + "<stateOrProvinceCode>CA</stateOrProvinceCode><countryCode>USA</countryCode>"
            + "<postalCode>94108</postalCode></billingAddress><shippingAddress><name>Mulesoft</name>"
            + "<line1>77 Geary St</line1><line2>Level 4</line2><city>San Francisco</city>"
            + "<stateOrProvinceCode>CA</stateOrProvinceCode><countryCode>USA</countryCode>"
            + "<postalCode>94108</postalCode></shippingAddress><order><orderItem><merchantSKU>1234</merchantSKU>"
            + "<quantity>500</quantity></orderItem><orderItem><merchantSKU>6789</merchantSKU>"
            + "<quantity>1500</quantity></orderItem><orderItem><merchantSKU>9998</merchantSKU>"
            + "<quantity>5000</quantity></orderItem></order></shippingOrder>"
            + "<orderReceivedStatus>true</orderReceivedStatus></ShippingOrderConfirmation>"
            + "</ns2:putShippingOrderResponse></soap:Body></soap:Envelope>";

    private SoapFixtures() {
    }

    public static ShippingOrder sampleShippingOrder() {
        Address address = new Address("Mulesoft", "77 Geary St", "Level 4", "San Francisco", "CA", "USA", "94108");
        return new ShippingOrder("1234", address, address, new Order(List.of(
                new OrderItem("1234", 500),
                new OrderItem("6789", 1500),
                new OrderItem("9998", 5000))));
    }

    public static String requestWithQuantity(String quantity) {
        return REQUEST.replace("<quantity>500</quantity>", "<quantity>" + quantity + "</quantity>");
    }
}
