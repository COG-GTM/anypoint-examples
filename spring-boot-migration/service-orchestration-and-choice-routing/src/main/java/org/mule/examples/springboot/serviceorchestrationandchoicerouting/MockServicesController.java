package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Element;

/** The bundled services: {@code samsungService} (SamsungServiceImpl) and {@code priceService} (ProductPrice). */
@RestController
public class MockServicesController {

    @PostMapping(path = "/samsung/orders", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> samsungPurchase(@RequestBody String soap) {
        Element request = OrderSoap.child(
                OrderSoap.first(OrderSoap.parseXml(soap).getDocumentElement(), "purchase"), "orderRequest");
        int quantity = Integer.parseInt(OrderSoap.text(request, "quantity").trim());
        Integer price = 2550 * quantity;
        StringBuilder b = new StringBuilder("<soap:Envelope xmlns:soap=\"" + OrderSoap.SOAP_NS + "\"><soap:Body>")
                .append("<ns2:purchaseResponse xmlns:ns2=\"" + SamsungClient.SAMSUNG_NS + "\"><orderResponse>")
                .append("<id>1</id><price>").append(price).append("</price><result>ACCEPTED</result>")
                .append("</orderResponse></ns2:purchaseResponse></soap:Body></soap:Envelope>");
        return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(b.toString());
    }

    @GetMapping(path = "/api/prices/{productId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String productPrice(@PathVariable String productId) {
        return "159";
    }
}
