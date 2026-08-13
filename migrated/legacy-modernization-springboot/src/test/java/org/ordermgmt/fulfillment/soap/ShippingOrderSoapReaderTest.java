package org.ordermgmt.fulfillment.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.ordermgmt.fulfillment.SoapFixtures;
import org.ordermgmt.fulfillment.model.ShippingOrder;

class ShippingOrderSoapReaderTest {

    private final ShippingOrderSoapReader reader = new ShippingOrderSoapReader();

    @Test
    void readsTheSampleRequestOfTheMuleApplication() {
        ShippingOrder shippingOrder = reader.read(SoapFixtures.REQUEST);

        assertThat(shippingOrder.shippingId()).isEqualTo("1234");
        assertThat(shippingOrder.billingAddress().line1()).isEqualTo("77 Geary St");
        assertThat(shippingOrder.shippingAddress().postalCode()).isEqualTo("94108");
        assertThat(shippingOrder.order().orderItemList()).hasSize(3);
        assertThat(shippingOrder.order().orderItemList().get(1).merchantSKU()).isEqualTo("6789");
        assertThat(shippingOrder.order().orderItemList().get(1).quantity()).isEqualTo(1500);
    }

    @Test
    void rejectsAnEnvelopeWithoutAPutShippingOrderElement() {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body><somethingElse/></soapenv:Body>
                </soapenv:Envelope>""";

        assertThatThrownBy(() -> reader.read(envelope))
                .isInstanceOf(InvalidSoapRequestException.class)
                .hasMessageContaining("putShippingOrder");
    }

    @Test
    void rejectsANonNumericQuantity() {
        assertThatThrownBy(() -> reader.read(SoapFixtures.requestWithQuantity("many")))
                .isInstanceOf(InvalidSoapRequestException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void rejectsMalformedXml() {
        assertThatThrownBy(() -> reader.read("<soapenv:Envelope>"))
                .isInstanceOf(InvalidSoapRequestException.class);
    }
}
