package com.example.leagues;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Leagues REST API.
 *
 * <p>Spring Boot replacement for the MuleSoft {@code rest-api-with-apikit}
 * application. The Mule {@code http:listener} on {@code /api/*} backed by an
 * APIkit router is replaced by Spring MVC controllers; the embedded Tomcat
 * server replaces the Mule runtime.
 */
@SpringBootApplication
public class LeaguesApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaguesApplication.class, args);
    }
}
