package org.mule.examples.springboot.importleadsintosalesforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot port of the Mule 3.8 example {@code import-leads-into-salesforce} ({@code import-leads-into-salesforce.xml}). */
@SpringBootApplication
public class ImportLeadsIntoSalesforceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImportLeadsIntoSalesforceApplication.class, args);
    }
}
