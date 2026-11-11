package org.mule.examples.springboot.legacymodernization.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"shippingOrder", "orderReceivedStatus"})
public class ShippingOrderConfirmation {
    public ShippingOrder shippingOrder;
    public boolean orderReceivedStatus;
}
