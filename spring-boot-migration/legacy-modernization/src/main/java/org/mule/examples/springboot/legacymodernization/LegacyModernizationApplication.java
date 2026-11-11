package org.mule.examples.springboot.legacymodernization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LegacyModernizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyModernizationApplication.class, args);
    }
}
