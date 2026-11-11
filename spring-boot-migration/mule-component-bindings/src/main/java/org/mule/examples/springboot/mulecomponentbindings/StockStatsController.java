package org.mule.examples.springboot.mulecomponentbindings;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code StockServiceFlow} (Jersey {@code /api/stockStats}) and {@code mule-component-bindingsFlow1} ({@code /}, fixed
 * AAPL Twitter search).
 */
@RestController
public class StockStatsController {

    private final StockStatsService service;
    private final TwitterClient twitter;

    public StockStatsController(StockStatsService service, TwitterClient twitter) {
        this.service = service;
        this.twitter = twitter;
    }

    @GetMapping(path = "/api/stockStats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Model.StockStats> stockStats(@RequestParam(required = false) String stock,
                                                       @RequestParam(required = false) String date) {
        Model.StockStats stats = service.search(stock, date);
        return stats == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(stats);
    }

    @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Model.Tweet> aaplTweets() {
        return twitter.search("AAPL", "2014-10-26", "2014-11-04");
    }

    /** WebApplicationException(Response.status(s).entity(message)) under @Produces(APPLICATION_JSON). */
    @ExceptionHandler(StockStatsService.StockStatsException.class)
    public ResponseEntity<String> error(StockStatsService.StockStatsException e) {
        return ResponseEntity.status(e.status()).contentType(MediaType.APPLICATION_JSON).body(e.getMessage());
    }
}
