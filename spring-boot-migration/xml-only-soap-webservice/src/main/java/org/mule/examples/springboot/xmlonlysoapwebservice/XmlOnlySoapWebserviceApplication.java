package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration;

/** Spring-WS is configured by {@link WebServiceConfiguration} (three exact service paths instead of Boot's {@code /services/*}). */
@SpringBootApplication(exclude = WebServicesAutoConfiguration.class)
public class XmlOnlySoapWebserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(XmlOnlySoapWebserviceApplication.class, args);
    }
}
