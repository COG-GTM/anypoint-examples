package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UploadToFtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(UploadToFtpApplication.class, args);
    }
}
