package org.mule.examples.springboot.importingacsvfileintomongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CsvToMongoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsvToMongoApplication.class, args);
    }
}
