package org.mule.examples.springboot.salesforcedataretrieval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot port of the Mule 3.8 example {@code salesforce-data-retrieval} ({@code salesforce-id-retrieval.xml}). */
@SpringBootApplication
public class SalesforceDataRetrievalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesforceDataRetrievalApplication.class, args);
    }
}
