package org.mule.examples.springboot.sapdataretrieval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SapDataRetrievalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SapDataRetrievalApplication.class, args);
    }
}
