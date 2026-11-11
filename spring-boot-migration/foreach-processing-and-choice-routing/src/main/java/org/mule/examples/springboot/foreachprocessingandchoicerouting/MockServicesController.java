package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The bundled mock services ({@code TheCreditAgencyService}, {@code Bank1Flow}..{@code Bank5Flow}): random credit
 * profile and per-bank random rates (sorted, indexed by credit score), as in DefaultCreditAgency and Bank.
 */
@RestController
public class MockServicesController {

    private final Map<String, double[]> bankRates = new ConcurrentHashMap<>();

    @PostMapping("/mule/TheCreditAgencyService")
    public Model.CreditProfile creditProfile(@RequestBody Model.Customer customer) {
        return new Model.CreditProfile((int) (Math.random() * 600 + 300), (int) (Math.random() * 19 + 1));
    }

    @PostMapping("/mule/TheBank{n:[1-5]}")
    public Model.LoanQuote loanQuote(@PathVariable("n") int n, @RequestBody Model.LoanBrokerQuoteRequest request) {
        double[] rates = bankRates.computeIfAbsent("Bank #" + n, k -> {
            double[] r = {Math.random() * 10, Math.random() * 10, Math.random() * 10};
            Arrays.sort(r);
            return r;
        });
        int score = request.creditProfile().creditScore();
        int index = score < 500 ? 2 : score < 700 ? 1 : 0;
        return new Model.LoanQuote("Bank #" + n, rates[index]);
    }
}
