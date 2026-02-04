package com.example.helloworld.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class HelloWorldServiceTest {

    @Autowired
    private HelloWorldService helloWorldService;

    @Test
    void getHelloWorldMessage_shouldReturnHelloWorld() {
        String result = helloWorldService.getHelloWorldMessage();
        assertEquals("Hello World", result);
    }
}
