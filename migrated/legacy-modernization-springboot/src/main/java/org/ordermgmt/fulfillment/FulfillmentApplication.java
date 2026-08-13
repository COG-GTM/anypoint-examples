package org.ordermgmt.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point of the Spring Boot service that replaces the Mule 3
 * {@code Fulfillment_LegacySystemModernization} flow.
 */
@SpringBootApplication
@EnableAsync
public class FulfillmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FulfillmentApplication.class, args);
    }
}
