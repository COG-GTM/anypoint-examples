package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Java replacement for the two DataWeave scripts of {@code dw:transform-message} in books.xml. */
public final class OrderMapper {

    private OrderMapper() {
    }

    public record Item(BigDecimal price, String title, List<String> authors, BigDecimal year) {
    }

    public record CurrencyRatio(String currency, BigDecimal ratio) {
    }

    @JsonPropertyOrder({"price", "currency"})
    public record Price(BigDecimal price, String currency) {
    }

    public record Author(String author) {
    }

    @JsonPropertyOrder({"title", "prices", "authors"})
    public record Order(String title, List<Price> prices, List<Author> authors) {
    }

    public record Orders(List<Order> orders) {
    }

    /**
     * <pre>
     * orders: payload.orders.*item filter $.properties.year > 2004 map using (item = $) {
     *   title: $.properties.title,
     *   prices: flowVars.currencies.USD map { price: $.ratio * item.price, currency: $.currency },
     *   authors: $.properties.authors distinctBy $ map { author: $ } }
     * </pre>
     */
    public static Orders toOrders(List<Item> items, Map<String, List<CurrencyRatio>> currencies) {
        List<CurrencyRatio> usd = currencies.getOrDefault("USD", List.of());
        List<Order> orders = new ArrayList<>();
        for (Item item : items) {
            if (item.year() == null || item.year().compareTo(BigDecimal.valueOf(2004)) <= 0) {
                continue;
            }
            List<Price> prices = usd.stream()
                    .map(c -> new Price(c.ratio().multiply(item.price()), c.currency()))
                    .toList();
            List<Author> authors = new LinkedHashSet<>(item.authors()).stream().map(Author::new).toList();
            orders.add(new Order(item.title(), prices, authors));
        }
        return new Orders(orders);
    }

    /**
     * <pre>
     * %output application/csv
     * [{ totalNumberOfItems: sizeOf payload.orders.*item, totalAmount: sum payload.orders.*item.price,
     *    averageSellingPrice: avg payload.orders.*item.price }]
     * </pre>
     */
    public static String toReportCsv(List<Item> items) {
        BigDecimal total = items.stream().map(Item::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        String average = items.isEmpty() ? ""
                : total.divide(BigDecimal.valueOf(items.size()), MathContext.DECIMAL128).stripTrailingZeros()
                        .toPlainString();
        return "totalNumberOfItems,totalAmount,averageSellingPrice\n"
                + items.size() + "," + total.toPlainString() + "," + average + "\n";
    }
}
