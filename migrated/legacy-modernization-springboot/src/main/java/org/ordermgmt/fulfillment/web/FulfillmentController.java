package org.ordermgmt.fulfillment.web;

import org.ordermgmt.fulfillment.model.ShippingOrder;
import org.ordermgmt.fulfillment.model.ShippingOrderConfirmation;
import org.ordermgmt.fulfillment.service.FulfillmentService;
import org.ordermgmt.fulfillment.soap.ShippingOrderSoapReader;
import org.ordermgmt.fulfillment.soap.ShippingOrderSoapWriter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces the Mule {@code http:listener} on {@code POST /OrderFulfillment} (port 1080) together
 * with the {@code cxf:jaxws-service} that exposed {@code org.ordermgmt.IFulfillment}. The path,
 * method and SOAP payload shapes are preserved so existing callers do not change.
 */
@RestController
public class FulfillmentController {

    private final ShippingOrderSoapReader reader;
    private final ShippingOrderSoapWriter writer;
    private final FulfillmentService fulfillmentService;

    public FulfillmentController(ShippingOrderSoapReader reader,
                                 ShippingOrderSoapWriter writer,
                                 FulfillmentService fulfillmentService) {
        this.reader = reader;
        this.writer = writer;
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping(path = "/OrderFulfillment", produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> putShippingOrder(@RequestBody(required = false) String soapEnvelope) {
        ShippingOrder shippingOrder = reader.read(soapEnvelope);
        ShippingOrderConfirmation confirmation = fulfillmentService.putShippingOrder(shippingOrder);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_XML)
                .body(writer.writeConfirmation(confirmation));
    }
}
