package org.mule.examples.springboot.cachescopewithfibonacci;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code cache-exampleFlow1}: {@code http:listener path="fibonacci"} and the {@code n < 20} choice. */
@RestController
public class FibonacciController {

    private final FibonacciService service;

    public FibonacciController(FibonacciService service) {
        this.service = service;
    }

    @RequestMapping(path = "/fibonacci", produces = MediaType.TEXT_PLAIN_VALUE)
    public String fibonacci(@RequestParam(name = "n", required = false) String n,
                            @RequestParam(name = "nocache", required = false) String nocacheParam,
                            @RequestHeader(name = "nocache", required = false) String nocacheHeader) {
        if (n == null) {
            throw new IllegalArgumentException("Query parameter 'n' is required");
        }
        // MEL coerces the String parameter to a number for "n < 20".
        if (new BigDecimal(n.trim()).compareTo(BigDecimal.valueOf(20)) >= 0) {
            return "ERROR: n must be less than 20";
        }
        FibonacciResult result = service.calculate(n, nocacheHeader != null, nocacheParam != null);
        return "Fibonacci(" + n + ") = " + result.value() + "\nCOST: " + result.cost();
    }

    /** Non-numeric or missing {@code n}: the MEL evaluation fails and Mule answers 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String invalidInput(IllegalArgumentException e) {
        return e.getMessage();
    }
}
