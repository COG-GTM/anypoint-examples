package org.mule.examples.springboot.mulecomponentbindings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Table-driven parity for StockStatsResourceImpl.search with the collaborators mocked as in MuleComponentBindingsIT:
 * validation, the paging loop of the Twitter binding, truncation and sentiment counting.
 */
class StockStatsTableTest {

    @ParameterizedTest(name = "stock={0} date={1} -> {2} {3}")
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "null | 2012-10-26 | 400 | stock is required",
            "'  ' | 2012-10-26 | 400 | stock is required",
            "AAPL | null       | 400 | date is required",
            "AAPL | ''         | 400 | date is required",
            "AAPL | 2012-1-26  | 400 | date format must be YYYY-MM-DD",
            "AAPL | 26/10/2012 | 400 | date format must be YYYY-MM-DD",
            "AAPL | abcdefghij | 400 | date format must be YYYY-MM-DD",
            "NOPE | 2012-10-26 | 400 | Invalid stock symbol NOPE",
            "BOOM | 2012-10-26 | 500 | stock service down"
    })
    void validationAndErrors(String stock, String date, int status, String message) {
        StockStatsService service = service(0, 0, List.of());
        assertThatThrownBy(() -> service.search(stock, date))
                .isInstanceOfSatisfying(StockStatsService.StockStatsException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(status);
                    assertThat(e.getMessage()).isEqualTo(message);
                });
    }

    @ParameterizedTest(name = "pages of {0} x {1} -> loaded={2} more={3} calls={4}")
    @CsvSource(delimiter = '|', value = {
            "0   | 0  | 0    | false | 1",
            "1   | 1  | 1    | false | 2",
            "100 | 10 | 1000 | false | 11",
            "100 | 11 | 1000 | true  | 11",
            "100 | 50 | 1000 | true  | 11",
            "15  | 70 | 1000 | true  | 67",
            "999 | 2  | 1000 | true  | 2"
    })
    void pagingAndTruncation(int pageSize, int pages, int loaded, boolean more, int calls) {
        List<Integer> callLog = new ArrayList<>();
        StockStatsService service = service(pageSize, pages, callLog);
        Model.StockStats stats = service.search("AAPL", "2012-10-26");
        assertThat(stats.socialStats.tweetsLoaded).isEqualTo(loaded);
        assertThat(stats.socialStats.moreTweets).isEqualTo(more);
        assertThat(callLog).hasSize(calls);
        assertThat(stats.socialStats.positiveTweets + stats.socialStats.negativeTweets
                + stats.socialStats.neutralTweets).isEqualTo(loaded);
    }

    @ParameterizedTest(name = "lenient date {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "2012-10-26 | 2012-10-26",
            "2012-13-01 | 2013-01-01",
            "2012-02-30 | 2012-03-01"
    })
    void lenientDateParsingLikeSimpleDateFormat(String input, String closingDate) {
        assertThat(service(0, 0, List.of()).search("AAPL", input).closingDate).isEqualTo(closingDate);
    }

    @ParameterizedTest(name = "polarity {0} -> {1}")
    @CsvSource(delimiter = '|', value = {"0 | NEGATIVE", "2 | NEUTRAL", "4 | POSITIVE", "-1 | POSITIVE"})
    void sentimentCounting(int polarity, Model.Sentiment sentiment) {
        Model.SocialStats stats = new Model.SocialStats();
        Model.Tweet tweet = new Model.Tweet("1", "t");
        tweet.setSentiment(polarity == 0 ? Model.Sentiment.NEGATIVE
                : polarity == 2 ? Model.Sentiment.NEUTRAL : Model.Sentiment.POSITIVE);
        stats.addTweet(tweet);
        assertThat(tweet.getSentiment()).isEqualTo(sentiment);
        assertThat(new int[] {stats.negativeTweets, stats.neutralTweets, stats.positiveTweets})
                .isEqualTo(sentiment == Model.Sentiment.NEGATIVE ? new int[] {1, 0, 0}
                        : sentiment == Model.Sentiment.NEUTRAL ? new int[] {0, 1, 0} : new int[] {0, 0, 1});
    }

    /** Twitter mock returning {@code pages} non-empty pages of {@code pageSize} tweets, then empty pages. */
    private static StockStatsService service(int pageSize, int pages, List<Integer> callLog) {
        Services.StockService stocks = (stock, date) -> {
            if ("NOPE".equals(stock)) {
                throw new Services.InvalidStockSymbol(stock);
            }
            if ("BOOM".equals(stock)) {
                throw new RuntimeException("stock service down");
            }
            Model.StockStats stats = new Model.StockStats();
            stats.closingDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
            return stats;
        };
        Services.TwitterService twitter = (String stock, Date date, int size, int page) -> {
            if (callLog instanceof ArrayList) {
                callLog.add(page);
            }
            return page <= pages
                    ? IntStream.range(0, pageSize).mapToObj(i -> new Model.Tweet(page + "-" + i, "t")).toList()
                    : List.of();
        };
        return new StockStatsService(stocks, twitter, tweets -> { });
    }
}
