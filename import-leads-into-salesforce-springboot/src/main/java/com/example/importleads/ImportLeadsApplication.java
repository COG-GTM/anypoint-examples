package com.example.importleads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ImportLeadsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImportLeadsApplication.class, args);
    }
}
