package org.mule.examples.springboot.importcontactsintosalesforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot port of the Mule 3.8 example {@code import-contacts-into-salesforce} ({@code contacts-to-SFDC.xml}). */
@SpringBootApplication
public class ImportContactsIntoSalesforceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImportContactsIntoSalesforceApplication.class, args);
    }
}
