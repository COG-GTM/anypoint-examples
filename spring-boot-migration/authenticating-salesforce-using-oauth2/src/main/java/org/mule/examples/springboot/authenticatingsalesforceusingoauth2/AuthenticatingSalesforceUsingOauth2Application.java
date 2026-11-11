package org.mule.examples.springboot.authenticatingsalesforceusingoauth2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot port of the Mule 3.8 example {@code authenticating-salesforce-using-oauth2} ({@code salesforce-oauth.xml}). */
@SpringBootApplication
public class AuthenticatingSalesforceUsingOauth2Application {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticatingSalesforceUsingOauth2Application.class, args);
    }
}
