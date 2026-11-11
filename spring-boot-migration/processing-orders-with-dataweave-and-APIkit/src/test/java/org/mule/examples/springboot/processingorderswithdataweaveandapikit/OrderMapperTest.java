package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderMapperTest {

    @Test
    void filtersByYearAndDeduplicatesAuthorsLikeDataWeaveDistinctBy() {
        var items = List.of(
                new OrderMapper.Item(new BigDecimal("10"), "Old", List.of("A"), new BigDecimal("2004")),
                new OrderMapper.Item(new BigDecimal("49.99"), "New", List.of("K", "J", "K"), new BigDecimal("2010")));
        var orders = OrderMapper.toOrders(items, Map.of("USD",
                List.of(new OrderMapper.CurrencyRatio("EUR", new BigDecimal("0.92")))));
        assertThat(orders.orders()).singleElement().satisfies(o -> {
            assertThat(o.title()).isEqualTo("New");
            assertThat(o.prices()).containsExactly(new OrderMapper.Price(new BigDecimal("45.9908"), "EUR"));
            assertThat(o.authors()).extracting(OrderMapper.Author::author).containsExactly("K", "J");
        });
    }

    @Test
    void reportAggregatesAllItemsIncludingFilteredOnes() {
        var items = List.of(
                new OrderMapper.Item(new BigDecimal("30"), "a", List.of(), new BigDecimal("2005")),
                new OrderMapper.Item(new BigDecimal("29.99"), "b", List.of(), new BigDecimal("2005")),
                new OrderMapper.Item(new BigDecimal("49.99"), "c", List.of(), new BigDecimal("2003")),
                new OrderMapper.Item(new BigDecimal("39.95"), "d", List.of(), new BigDecimal("2003")));
        assertThat(OrderMapper.toReportCsv(items))
                .isEqualTo("totalNumberOfItems,totalAmount,averageSellingPrice\n4,149.93,37.4825\n");
    }
}
