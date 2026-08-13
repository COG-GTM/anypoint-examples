package org.ordermgmt.fulfillment.soap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.ordermgmt.fulfillment.SoapFixtures;
import org.ordermgmt.fulfillment.model.ShippingOrderConfirmation;

class ShippingOrderSoapWriterTest {

    private final ShippingOrderSoapWriter writer = new ShippingOrderSoapWriter();

    @Test
    void reproducesTheResponseBytesOfTheMuleApplication() {
        String response = writer.writeConfirmation(
                new ShippingOrderConfirmation(SoapFixtures.sampleShippingOrder(), true));

        assertThat(response).isEqualTo(SoapFixtures.EXPECTED_REPLY);
    }

    @Test
    void writesASoapFault() {
        String fault = writer.writeFault("soap:Client", "Request body is empty");

        assertThat(fault).contains("<soap:Fault>")
                .contains("<faultcode>soap:Client</faultcode>")
                .contains("<faultstring>Request body is empty</faultstring>");
    }
}
