package org.mule.examples.springboot.exposingarestfulresourceusingthehttpconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RestfulResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestfulResourceApplication.class, args);
    }
}
