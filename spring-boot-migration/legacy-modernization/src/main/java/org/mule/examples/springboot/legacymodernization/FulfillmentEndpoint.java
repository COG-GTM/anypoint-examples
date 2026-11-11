package org.mule.examples.springboot.legacymodernization;

import org.mule.examples.springboot.legacymodernization.model.PutShippingOrder;
import org.mule.examples.springboot.legacymodernization.model.PutShippingOrderResponse;
import org.mule.examples.springboot.legacymodernization.model.ShippingOrder;
import org.mule.examples.springboot.legacymodernization.model.ShippingOrderConfirmation;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Fulfillment_LegacySystemModernization: {@code cxf:jaxws-service} + {@code org.ordermgmt.FulfillmentImpl} answer the
 * SOAP call synchronously, then the {@code async} scope writes the legacy CSV file.
 */
@Endpoint
public class FulfillmentEndpoint {

    private final LegacyFulfillmentFileWriter fileWriter;

    public FulfillmentEndpoint(LegacyFulfillmentFileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    @PayloadRoot(namespace = PutShippingOrder.NAMESPACE, localPart = "putShippingOrder")
    @ResponsePayload
    public PutShippingOrderResponse putShippingOrder(@RequestPayload PutShippingOrder request) {
        ShippingOrder shippingOrder = new ShippingOrder();
        shippingOrder.shippingId = request.shippingId;
        shippingOrder.billingAddress = request.billingAddress;
        shippingOrder.shippingAddress = request.shippingAddress;
        shippingOrder.order = request.order;

        ShippingOrderConfirmation confirmation = new ShippingOrderConfirmation();
        confirmation.shippingOrder = shippingOrder;
        confirmation.orderReceivedStatus = true;

        fileWriter.write(confirmation);

        PutShippingOrderResponse response = new PutShippingOrderResponse();
        response.confirmation = confirmation;
        return response;
    }
}
