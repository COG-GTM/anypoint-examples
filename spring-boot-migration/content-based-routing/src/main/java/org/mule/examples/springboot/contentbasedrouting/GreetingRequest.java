package org.mule.examples.springboot.contentbasedrouting;

/** The Mule {@code language} flow variable (the {@code language} query parameter, possibly absent). */
public record GreetingRequest(String language) {
}
