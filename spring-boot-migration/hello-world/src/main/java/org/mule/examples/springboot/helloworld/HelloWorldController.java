package org.mule.examples.springboot.helloworld;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HelloWorldFlow1: {@code http:listener path="helloWorld"} (any method) + {@code set-payload value="Hello World"}. */
@RestController
public class HelloWorldController {

    static final String PAYLOAD = "Hello World";

    @RequestMapping(path = "/helloWorld")
    public ResponseEntity<String> helloWorld() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(PAYLOAD);
    }
}
