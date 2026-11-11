package org.mule.examples.springboot.netsuitedataretrieval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NetsuiteDataRetrievalApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetsuiteDataRetrievalApplication.class, args);
    }
}
