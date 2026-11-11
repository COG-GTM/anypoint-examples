package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Table-driven parity for the manufacturer choice and the totalValue session variable. */
class OrderRoutingTableTest {

    @ParameterizedTest(name = "{0} -> samsung={1}")
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "Samsung | true", "samsung | false", "SAMSUNG | false", "' Samsung' | false",
            "Philips | false", "Sony | false", "'' | false", "null | false"
    })
    void manufacturerChoice(String manufacturer, boolean samsung) {
        assertThat(OrderOrchestrationFlowConfig.isSamsung(new Model.OrderItem("p", "n", manufacturer, 1)))
                .isEqualTo(samsung);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "ACCEPTED:2550                  | 2550",
            "ACCEPTED:12750,ACCEPTED:1590   | 14340",
            "REJECTED:0,ACCEPTED:318        | 318",
            "REJECTED:0                     | 0"
    })
    void totalValueSumsReceipts(String receipts, float total) {
        List<Model.OrderItem> items = new ArrayList<>();
        for (String r : receipts.split(",")) {
            String[] p = r.split(":");
            Model.OrderItem item = new Model.OrderItem("p", "n", "m", 1);
            item.purchaseReceipt = new Model.PurchaseReceipt(null, Model.Status.valueOf(p[0]), Float.parseFloat(p[1]));
            items.add(item);
        }
        assertThat(OrderFulfillment.totalValue(new Model.Order("1", null, items))).isEqualTo(total);
    }
}
