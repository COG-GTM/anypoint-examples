package org.mule.examples.springboot.mulecomponentbindings;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** StocklyticsStockService: GET historicalPrices/1.0 with api_key/format/stock/start/end headers. */
@Component
public class StocklyticsStockService implements Services.StockService {

    static final String NO_DATA = "Error: Stocklytics doesn't have data for that stock.";

    private final RestClient rest;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public StocklyticsStockService(RestClient.Builder builder, @Value("${stockstats.stocklytics.base-url}") String baseUrl,
                                   @Value("${stockstats.stocklytics.api-key:}") String apiKey) {
        this.rest = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public Model.StockStats getHistoricalPrices(String stock, Date closingDate) throws Services.InvalidStockSymbol {
        String day = new SimpleDateFormat("yyyy-MM-dd").format(closingDate);
        String[] result = rest.get().uri("/historicalPrices/1.0").accept(MediaType.APPLICATION_JSON)
                .header("api_key", apiKey).header("format", "JSON").header("stock", stock)
                .header("start", day).header("end", day)
                .exchange((req, res) -> new String[] {String.valueOf(res.getStatusCode().value()),
                        new String(res.getBody().readAllBytes())});
        if (!"200".equals(result[0])) {
            throw new RuntimeException("Stockylitics returned status " + result[0]);
        }
        if (result[1].contains(NO_DATA)) {
            throw new Services.InvalidStockSymbol(stock);
        }
        try {
            JsonNode node = mapper.readTree(result[1]).path(day);
            Model.StockStats stats = new Model.StockStats();
            stats.closingDate = day;
            stats.financialStats.open = node.path("open").asDouble();
            stats.financialStats.close = node.path("close").asDouble();
            stats.financialStats.high = node.path("high").asDouble();
            stats.financialStats.low = node.path("low").asDouble();
            stats.financialStats.volume = node.path("volume").asLong();
            return stats;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
