package org.mule.examples.springboot.legacymodernization.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrderItem {
    public String merchantSKU;
    public int quantity;
}
