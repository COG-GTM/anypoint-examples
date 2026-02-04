package org.mule.examples.contentbasedrouting;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/")
    public String getGreeting(@RequestParam(value = "language", required = false) String language) {
        return greetingService.getGreeting(language).greeting();
    }
}
