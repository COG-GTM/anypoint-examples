package org.mule.examples.springboot.importcontactsintomsdynamics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImportContactsIntoMsDynamicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImportContactsIntoMsDynamicsApplication.class, args);
    }
}
