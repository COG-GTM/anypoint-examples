package org.mule.examples.springboot.mulecomponentbindings;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Ports of stockstats.* (StockStats, FinancialStats, SocialStats, Tweet, Sentiment). */
public final class Model {

    private Model() {
    }

    public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

    public static class Tweet {
        private String id;
        private String text;
        private Sentiment sentiment;

        public Tweet() {
        }

        public Tweet(String id, String text) {
            this.id = id;
            this.text = text;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public Sentiment getSentiment() {
            return sentiment;
        }

        public void setSentiment(Sentiment sentiment) {
            this.sentiment = sentiment;
        }
    }

    @JsonPropertyOrder({"open", "close", "high", "low", "volume"})
    public static class FinancialStats {
        public double open;
        public double close;
        public double high;
        public double low;
        public long volume;
    }

    @JsonPropertyOrder({"tweetsLoaded", "moreTweets", "negativeTweets", "neutralTweets", "positiveTweets"})
    public static class SocialStats {
        public int tweetsLoaded;
        public boolean moreTweets;
        public int negativeTweets;
        public int neutralTweets;
        public int positiveTweets;

        /** SocialStats.addTweet: unclassified tweets count as positive. */
        public void addTweet(Tweet tweet) {
            tweetsLoaded++;
            if (tweet.getSentiment() == Sentiment.NEGATIVE) {
                negativeTweets++;
            } else if (tweet.getSentiment() == Sentiment.NEUTRAL) {
                neutralTweets++;
            } else {
                positiveTweets++;
            }
        }
    }

    @JsonPropertyOrder({"closingDate", "financialStats", "socialStats"})
    public static class StockStats {
        public String closingDate;
        public FinancialStats financialStats = new FinancialStats();
        public SocialStats socialStats = new SocialStats();
    }
}
