package org.mule.examples.springboot.helloworld;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HelloWorldFlow1: {@code http:listener path="helloWorld"} (any method) + {@code set-payload value="Hello World"}. */
@RestController
public class HelloWorldController {

    static final String PAYLOAD = "Hello World";

    @RequestMapping(path = "/helloWorld")
    public String helloWorld() {
        return PAYLOAD;
    }
}
