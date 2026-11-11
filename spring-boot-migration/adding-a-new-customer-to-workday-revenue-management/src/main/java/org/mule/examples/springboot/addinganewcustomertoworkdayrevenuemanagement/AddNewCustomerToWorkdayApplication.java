package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AddNewCustomerToWorkdayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddNewCustomerToWorkdayApplication.class, args);
    }
}
