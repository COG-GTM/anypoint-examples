package org.mule.examples.springboot.sapdataretrieval;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The APIkit router for {@code sap-api.raml} (listener path {@code /api/*}) and its three GET flows. */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class SapApiController {

    static final Map<String, String> NO_DATA = Map.of("message", "No data found.");

    private final SapClient sap;

    public SapApiController(SapClient sap) {
        this.sap = sap;
    }

    /** Flow {@code get:/customers:sap-api-config}. */
    @GetMapping("/customers")
    public Map<String, Object> customers(@RequestParam(required = false) String name,
                                         @RequestParam(name = "max_count", required = false) String maxCount) {
        return Map.of("customers", sap.findCustomers(SapRequests.CustomerFind.of(maxCount, name)));
    }

    /** Flow {@code get:/materials:sap-api-config}. */
    @GetMapping("/materials")
    public Map<String, Object> materials(@RequestParam(required = false) String name,
                                         @RequestParam(name = "max_count", required = false) String maxCount) {
        return Map.of("materials", orNoData(sap.getMaterials(SapRequests.MaterialGetList.of(maxCount, name))));
    }

    /** Flow {@code get:/salesOrders:sap-api-config}; RAML defaults are a single space. */
    @GetMapping("/salesOrders")
    public Map<String, Object> salesOrders(
            @RequestParam(name = "customer_number", defaultValue = " ") String customerNumber,
            @RequestParam(name = "sales_organization", defaultValue = " ") String salesOrganization) {
        return Map.of("salesOrders",
                orNoData(sap.getSalesOrders(new SapRequests.SalesOrderGetList(customerNumber, salesOrganization))));
    }

    private static Object orNoData(List<SapRow> rows) {
        return rows.isEmpty() ? NO_DATA : rows;
    }
}
