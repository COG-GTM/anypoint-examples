package org.mule.examples.springboot.legacymodernization.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Response wrapper; {@code @WebResult(name="ShippingOrderConfirmation")} names the single child element. */
@XmlRootElement(name = "putShippingOrderResponse", namespace = PutShippingOrder.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class PutShippingOrderResponse {
    @XmlElement(name = "ShippingOrderConfirmation")
    public ShippingOrderConfirmation confirmation;
}
