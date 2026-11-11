package org.mule.examples.springboot.legacymodernization.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** Request wrapper of the JAX-WS operation {@code IFulfillment.putShippingOrder} (document/literal wrapped). */
@XmlRootElement(name = "putShippingOrder", namespace = PutShippingOrder.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"shippingId", "billingAddress", "shippingAddress", "order"})
public class PutShippingOrder {
    public static final String NAMESPACE = "http://ordermgmt.org/";

    public String shippingId;
    public Address billingAddress;
    public Address shippingAddress;
    public Order order;
}
