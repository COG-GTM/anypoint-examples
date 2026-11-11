package org.mule.examples.springboot.mulecomponentbindings;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** StockStatsResourceImpl.search, line for line (lenient SimpleDateFormat, paging loop, truncation to 1000). */
@Service
public class StockStatsService {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    static final int MAX_RESULTS = 1000;
    static final int PAGE_SIZE = 100;

    private final Services.StockService stockService;
    private final Services.TwitterService twitterService;
    private final Services.SentimentService sentimentService;

    public StockStatsService(Services.StockService stockService, Services.TwitterService twitterService,
                             Services.SentimentService sentimentService) {
        this.stockService = stockService;
        this.twitterService = twitterService;
        this.sentimentService = sentimentService;
    }

    public Model.StockStats search(String stock, String dateStr) {
        try {
            if (stock == null || stock.isBlank()) {
                throw new StockStatsException(HttpStatus.BAD_REQUEST, "stock is required");
            }
            if (dateStr == null || dateStr.isBlank()) {
                throw new StockStatsException(HttpStatus.BAD_REQUEST, "date is required");
            }
            if (dateStr.length() != DATE_FORMAT.length()) {
                throw new StockStatsException(HttpStatus.BAD_REQUEST, "date format must be YYYY-MM-DD");
            }
            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            Date date;
            try {
                date = df.parse(dateStr);
            } catch (ParseException e) {
                throw new StockStatsException(HttpStatus.BAD_REQUEST, "date format must be YYYY-MM-DD");
            }
            Model.StockStats stockStats;
            try {
                stockStats = stockService.getHistoricalPrices(stock, date);
            } catch (Services.InvalidStockSymbol e) {
                throw new StockStatsException(HttpStatus.BAD_REQUEST, "Invalid stock symbol " + stock);
            }
            List<Model.Tweet> tweets = new ArrayList<>();
            int page = 1;
            while (true) {
                List<Model.Tweet> results = twitterService.search(stock, date, PAGE_SIZE, page);
                tweets.addAll(results);
                if (results.isEmpty()) {
                    break;
                }
                if (tweets.size() > MAX_RESULTS) {
                    stockStats.socialStats.moreTweets = true;
                    break;
                }
                page++;
            }
            while (tweets.size() > MAX_RESULTS) {
                tweets.remove(MAX_RESULTS);
            }
            sentimentService.classify(tweets);
            for (Model.Tweet tweet : tweets) {
                if (stockStats != null) {
                    stockStats.socialStats.addTweet(tweet);
                }
            }
            return stockStats;
        } catch (StockStatsException e) {
            throw e;
        } catch (Throwable e) {
            throw new StockStatsException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public static class StockStatsException extends RuntimeException {
        private final HttpStatus status;

        StockStatsException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus status() {
            return status;
        }
    }
}
