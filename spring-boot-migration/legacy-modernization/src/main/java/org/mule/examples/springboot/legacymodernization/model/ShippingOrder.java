package org.mule.examples.springboot.legacymodernization.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"shippingId", "billingAddress", "shippingAddress", "order"})
public class ShippingOrder {
    public String shippingId;
    public Address billingAddress;
    public Address shippingAddress;
    public Order order;
}
