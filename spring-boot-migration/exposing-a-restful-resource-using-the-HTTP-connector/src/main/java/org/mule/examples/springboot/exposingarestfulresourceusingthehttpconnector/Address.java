package org.mule.examples.springboot.exposingarestfulresourceusingthehttpconnector;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"streetAddress", "city", "state", "zipCode"})
public record Address(String streetAddress, String city, String state, String zipCode) {
}
