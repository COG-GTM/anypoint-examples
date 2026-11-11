package org.mule.examples.springboot.salesforcedatasynchronizationusingwatermarkingandbatchprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot port of the Mule 3.8 example
 * {@code salesforce-data-synchronization-using-watermarking-and-batch-processing} ({@code watermarking.xml}).
 */
@SpringBootApplication
public class SalesforceDataSynchronizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesforceDataSynchronizationApplication.class, args);
    }
}
