package org.ordermgmt.fulfillment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ordermgmt.fulfillment.SoapFixtures;
import org.ordermgmt.fulfillment.model.Address;
import org.ordermgmt.fulfillment.model.Order;
import org.ordermgmt.fulfillment.model.OrderItem;
import org.ordermgmt.fulfillment.model.ShippingOrder;

class ShippingOrderCsvTransformerTest {

    private final ShippingOrderCsvTransformer transformer = new ShippingOrderCsvTransformer();

    @Test
    void emitsAHeaderAndOneRowPerOrderItem() {
        String csv = transformer.toCsv(SoapFixtures.sampleShippingOrder());

        List<String> lines = csv.lines().toList();
        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).isEqualTo("MSKU,QTY,BillingAddressName,BillingAddressStreet,BillingAddrCity,"
                + "BillingAddrState,BillingAddrCountry,BillingAddrZipCode,ShippingAddrName,ShippingAddrStreet,"
                + "ShippingAddrCity,ShippingAddrState,ShippingAddrCountry,ShippingAddrZipCode,ShippingId");
        assertThat(lines.get(1)).isEqualTo("1234,500,Mulesoft,77 Geary St Level 4,San Francisco,CA,USA,94108,"
                + "Mulesoft,77 Geary St Level 4,San Francisco,CA,USA,94108,1234");
        assertThat(lines.get(3)).startsWith("9998,5000,");
    }

    @Test
    void concatenatesTheTwoStreetLinesWithASpace() {
        Address address = new Address("Acme", "1 Main St", "Suite 9", "Boston", "MA", "USA", "02101");
        ShippingOrder shippingOrder = new ShippingOrder("42", address, address,
                new Order(List.of(new OrderItem("SKU-1", 1))));

        assertThat(transformer.toCsv(shippingOrder).lines().toList().get(1))
                .contains("1 Main St Suite 9");
    }

    @Test
    void leavesTheStreetEmptyWhenALineIsMissing() {
        Address address = new Address("Acme", "1 Main St", null, "Boston", "MA", "USA", "02101");
        ShippingOrder shippingOrder = new ShippingOrder("42", address, address,
                new Order(List.of(new OrderItem("SKU-1", 1))));

        assertThat(transformer.toCsv(shippingOrder).lines().toList().get(1))
                .isEqualTo("SKU-1,1,Acme,,Boston,MA,USA,02101,Acme,,Boston,MA,USA,02101,42");
    }

    @Test
    void quotesValuesContainingTheSeparator() {
        Address address = new Address("Acme, Inc.", "1 Main St", "Suite 9", "Boston", "MA", "USA", "02101");
        ShippingOrder shippingOrder = new ShippingOrder("42", address, address,
                new Order(List.of(new OrderItem("SKU-1", 1))));

        assertThat(transformer.toCsv(shippingOrder)).contains("\"Acme, Inc.\"");
    }

    @Test
    void emitsOnlyTheHeaderWhenThereAreNoOrderItems() {
        ShippingOrder shippingOrder = new ShippingOrder("42", null, null, new Order(List.of()));

        assertThat(transformer.toCsv(shippingOrder).lines().toList()).hasSize(1);
    }
}
