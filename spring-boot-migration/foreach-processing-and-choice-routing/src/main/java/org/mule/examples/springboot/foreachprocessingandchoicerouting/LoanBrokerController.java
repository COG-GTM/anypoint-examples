package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code http:listener path="/*"} on port 11081 with the required-fields choice and the catch-exception-strategy. */
@RestController
public class LoanBrokerController {

    static final String INCOMPLETE = "Error: incomplete request";
    static final String FAILED = "Error processing loan request";

    private static final Logger log = LoggerFactory.getLogger(LoanBrokerController.class);

    private final LoanBrokerGateway gateway;

    public LoanBrokerController(LoanBrokerGateway gateway) {
        this.gateway = gateway;
    }

    @RequestMapping(path = "/**", produces = MediaType.TEXT_PLAIN_VALUE)
    public String loan(@RequestParam(required = false) String name, @RequestParam(required = false) String ssn,
                       @RequestParam(required = false) String amount, @RequestParam(required = false) String term) {
        if (name == null || ssn == null || amount == null || term == null) {
            return INCOMPLETE;
        }
        try {
            return gateway.lowestQuote(new Model.CustomerQuoteRequest(new Model.Customer(name, Integer.parseInt(ssn)),
                    Integer.parseInt(amount), Integer.parseInt(term)));
        } catch (RuntimeException e) {
            log.warn("Loan request failed", e);
            return FAILED;
        }
    }
}
