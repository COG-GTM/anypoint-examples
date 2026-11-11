package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Clock;

import org.springframework.context.annotation.Bean;

/** Spring Boot port of the Mule 3.8 example {@code salesforce-to-MySQL-DB-using-Batch-Processing} ({@code salesforce-to-database.xml}). */
@SpringBootApplication
public class SalesforceToMySqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesforceToMySqlApplication.class, args);
    }

    /** Time source for the watermark default expression. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
