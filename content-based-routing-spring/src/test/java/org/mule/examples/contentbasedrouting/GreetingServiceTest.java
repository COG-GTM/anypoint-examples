package org.mule.examples.contentbasedrouting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingServiceTest {

    private final GreetingService greetingService = new GreetingService();

    @Test
    void shouldReturnHolaForSpanish() {
        GreetingService.GreetingResponse response = greetingService.getGreeting("Spanish");
        assertEquals("Hola!", response.greeting());
        assertEquals("Spanish", response.language());
    }

    @Test
    void shouldReturnBonjourForFrench() {
        GreetingService.GreetingResponse response = greetingService.getGreeting("French");
        assertEquals("Bonjour!", response.greeting());
        assertEquals("French", response.language());
    }

    @Test
    void shouldReturnHelloForNullLanguage() {
        GreetingService.GreetingResponse response = greetingService.getGreeting(null);
        assertEquals("Hello!", response.greeting());
        assertEquals("English", response.language());
    }

    @Test
    void shouldReturnHelloForEmptyLanguage() {
        GreetingService.GreetingResponse response = greetingService.getGreeting("");
        assertEquals("Hello!", response.greeting());
        assertEquals("English", response.language());
    }

    @Test
    void shouldReturnHelloForUnknownLanguage() {
        GreetingService.GreetingResponse response = greetingService.getGreeting("German");
        assertEquals("Hello!", response.greeting());
        assertEquals("English", response.language());
    }
}
