package org.mule.examples.springboot.exposingarestfulresourceusingthehttpconnector;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Mirrors {@code org.mule.examples.Person}; {@code age} is a primitive int, so it defaults to 0. */
@JsonPropertyOrder({"firstname", "lastname", "address", "age"})
public record Person(String firstname, String lastname, Address address, int age) {
}
