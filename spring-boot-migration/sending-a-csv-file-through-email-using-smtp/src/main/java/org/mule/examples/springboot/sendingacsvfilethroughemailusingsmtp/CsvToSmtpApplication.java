package org.mule.examples.springboot.sendingacsvfilethroughemailusingsmtp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CsvToSmtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsvToSmtpApplication.class, args);
    }
}
