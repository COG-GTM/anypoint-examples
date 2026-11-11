package org.mule.examples.springboot.mulecomponentbindings;

import java.util.Date;
import java.util.List;

/** The three collaborator interfaces of StockStatsResourceImpl (stockstats.impl.*). */
public final class Services {

    private Services() {
    }

    public interface StockService {
        Model.StockStats getHistoricalPrices(String stock, Date closingDate) throws InvalidStockSymbol;
    }

    public interface TwitterService {
        List<Model.Tweet> search(String stock, Date date, int pageSize, int page);
    }

    public interface SentimentService {
        void classify(List<Model.Tweet> tweets);
    }

    public static class InvalidStockSymbol extends Exception {
        public InvalidStockSymbol(String stock) {
            super(stock);
        }
    }
}
