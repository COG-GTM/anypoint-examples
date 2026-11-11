package org.mule.examples.springboot.soapwebservicesecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration;

/** Spring-WS is configured by {@link WebServiceConfiguration} (one exact path per secured service). */
@SpringBootApplication(exclude = WebServicesAutoConfiguration.class)
public class SoapWebserviceSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoapWebserviceSecurityApplication.class, args);
    }
}
