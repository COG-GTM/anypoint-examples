package org.mule.examples.springboot.webserviceconsumer;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mule flows {@code orderTshirt} ({@code /orders}) and {@code listInventory} ({@code /inventory}). */
@RestController
class TshirtController {

    private final TshirtServiceClient tshirtService;

    TshirtController(TshirtServiceClient tshirtService) {
        this.tshirtService = tshirtService;
    }

    /** JSON → {@code ns0#OrderTshirt} + {@code AuthenticationHeader} → {@code OrderTshirt} → {@code payload} as JSON. */
    @RequestMapping(path = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> orderTshirt(@RequestBody JsonNode order) {
        String request = DataWeaveJson.toXmlElement("ns0", TshirtServiceClient.NS, "OrderTshirt", order);
        return ResponseEntity.ok(DataWeaveJson.document(tshirtService.orderTshirt(request)));
    }

    /** {@code ListInventory} → {@code payload.ns0#ListInventoryResponse} as JSON. */
    @RequestMapping(path = "/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> listInventory() {
        return ResponseEntity.ok(DataWeaveJson.content(tshirtService.listInventory()));
    }
}
