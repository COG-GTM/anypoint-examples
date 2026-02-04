package com.example.helloworld.controller;

import com.example.helloworld.service.HelloWorldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldController.class);

    private final HelloWorldService helloWorldService;

    public HelloWorldController(HelloWorldService helloWorldService) {
        this.helloWorldService = helloWorldService;
    }

    @GetMapping("/helloWorld")
    public String helloWorld() {
        logger.info("Received request to /helloWorld endpoint");
        String response = helloWorldService.getHelloWorldMessage();
        logger.debug("Returning response: {}", response);
        return response;
    }
}
