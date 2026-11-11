package org.mule.examples.springboot.legacymodernization.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Address {
    public String name;
    public String line1;
    public String line2;
    public String city;
    public String stateOrProvinceCode;
    public String countryCode;
    public String postalCode;
}
