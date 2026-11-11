package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GetCustomerListFromNetsuiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(GetCustomerListFromNetsuiteApplication.class, args);
    }
}
