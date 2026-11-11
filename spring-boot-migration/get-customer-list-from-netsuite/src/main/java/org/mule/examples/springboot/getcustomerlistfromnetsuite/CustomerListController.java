package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mule flow {@code get-customer-list-from-netsuiteFlow}: HTTP listener on {@code /customers}. */
@RestController
public class CustomerListController {

    private final NetSuiteQueryClient netSuite;
    private final CustomerTableRenderer renderer;

    public CustomerListController(NetSuiteQueryClient netSuite, CustomerTableRenderer renderer) {
        this.netSuite = netSuite;
        this.renderer = renderer;
    }

    @GetMapping("/customers")
    public ResponseEntity<String> customers(@RequestParam(required = false) String lastName) {
        if (lastName == null) {
            // <expression-filter expression="#[message.inboundProperties.'http.query.params'.lastName != null]"/>
            return ResponseEntity.ok().build();
        }
        List<NetSuiteCustomer> customers = netSuite.query(customerQuery(lastName));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(renderer.render(customers));
    }

    static String customerQuery(String lastName) {
        return "SELECT email,firstName,lastName FROM CUSTOMER WHERE lastName like '"
                + lastName.replace("'", "''") + "%' ORDER BY lastName ASC";
    }
}
