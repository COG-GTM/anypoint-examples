package org.mule.examples.springboot.netsuitedataretrieval;

import java.util.List;
import java.util.Set;
import org.mule.examples.springboot.netsuitedataretrieval.NetSuiteSearch.CustomerSearch;
import org.mule.examples.springboot.netsuitedataretrieval.NetSuiteSearch.ItemSupplyPlanSearchBasic;
import org.mule.examples.springboot.netsuitedataretrieval.NetSuiteSearch.OpportunitySearch;
import org.mule.examples.springboot.netsuitedataretrieval.NetSuiteSearch.SearchLongField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The APIkit router for {@code netsuite-api.raml} (listener path {@code /api/*}) and its three GET flows. */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class NetSuiteApiController {

    private static final Logger log = LoggerFactory.getLogger(NetSuiteApiController.class);

    static final Set<String> QUANTITY_OPERATORS = Set.of(
            "LESS_THAN", "GREATER_THAN", "EQUAL_TO", "LESS_THAN_OR_EQUAL_TO", "GREATER_THAN_OR_EQUAL_TO");

    private final NetSuiteSearchClient netSuite;

    public NetSuiteApiController(NetSuiteSearchClient netSuite) {
        this.netSuite = netSuite;
    }

    /** Flow {@code get:/customers:netsuite-api-config}. */
    @GetMapping("/customers")
    public List<NetSuiteRecord> customers(@RequestParam(required = false) String name) {
        List<NetSuiteRecord> customers = netSuite.searchCustomers(CustomerSearch.companyNameStartsWith(name));
        log.info("Get customers completes successfully.");
        return customers;
    }

    /** Flow {@code get:/items:netsuite-api-config}; RAML defaults quantity=0, operator=GREATER_THAN. */
    @GetMapping("/items")
    public List<NetSuiteRecord> items(@RequestParam(defaultValue = "0") String quantity,
                                      @RequestParam(defaultValue = "GREATER_THAN") String operator) {
        long value;
        try {
            value = Long.parseLong(quantity);
        } catch (NumberFormatException e) {
            throw new ApiKitBadRequestException("quantity must be an integer");
        }
        if (!QUANTITY_OPERATORS.contains(operator)) {
            throw new ApiKitBadRequestException("operator must be one of " + QUANTITY_OPERATORS);
        }
        List<NetSuiteRecord> items =
                netSuite.searchItemSupplyPlans(new ItemSupplyPlanSearchBasic(new SearchLongField(operator, value)));
        log.info("Get items completes successfully.");
        return items;
    }

    /** Flow {@code get:/oportunities:netsuite-api-config} (mapped to resource {@code /opportunities}). */
    @GetMapping("/opportunities")
    public List<NetSuiteRecord> opportunities(@RequestParam(required = false) String title) {
        List<NetSuiteRecord> opportunities = netSuite.searchOpportunities(OpportunitySearch.titleStartsWith(title));
        log.info("Get oportunities completes successfully.");
        return opportunities;
    }
}
