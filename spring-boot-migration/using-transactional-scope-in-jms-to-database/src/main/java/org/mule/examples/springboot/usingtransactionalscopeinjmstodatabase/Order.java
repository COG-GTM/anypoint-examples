package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** org.mule.examples.Order, bound from {@code <order><itemId/><itemUnits/><customerId/></order>}. */
@JacksonXmlRootElement(localName = "order")
public record Order(int itemId, int itemUnits, int customerId) {

    @Override
    public String toString() {
        return "Order [item_id=" + itemId + ", item_units=" + itemUnits + ", customer_id=" + customerId + "]";
    }
}
