package org.mule.examples.springboot.legacymodernization.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Order {
    @XmlElement(name = "orderItem")
    public List<OrderItem> orderItemList = new ArrayList<>();
}
