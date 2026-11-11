package org.mule.examples.springboot.parity.sample;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Minimal app used only to demonstrate and self-test the parity harness. It is not a migration
 * (the real hello-world module belongs to Ticket 9).
 */
@SpringBootApplication
public class SampleHarnessApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleHarnessApplication.class, args);
    }

    @RestController
    static class SampleController {

        private final RestClient crm;

        SampleController(RestClient.Builder builder, @Value("${sample.crm.base-url}") String crmBaseUrl) {
            this.crm = builder.baseUrl(crmBaseUrl).build();
        }

        /** Mirrors hello-world/HelloWorldFlow1: listener on /helloWorld, set-payload "Hello World". */
        @GetMapping(path = "/helloWorld", produces = MediaType.TEXT_PLAIN_VALUE)
        String helloWorld() {
            return "Hello World";
        }

        /** Maps the inbound customer, forwards it to a downstream CRM and returns the CRM id. */
        @PostMapping(path = "/sample/customers", consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseStatus(HttpStatus.CREATED)
        Map<String, Object> createCustomer(@RequestBody Map<String, String> in) {
            Map<String, String> outbound = Map.of(
                    "name", in.get("firstName") + " " + in.get("lastName"),
                    "email", in.get("email").toLowerCase());
            @SuppressWarnings("unchecked")
            Map<String, Object> created = crm.post().uri("/crm/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(outbound)
                    .retrieve()
                    .body(Map.class);
            return Map.of("customerId", created.get("id"), "status", "CREATED");
        }

        @GetMapping(path = "/sample/greeting", produces = MediaType.APPLICATION_XML_VALUE)
        String greeting() {
            return "<ns2:greeting xmlns:ns2=\"urn:sample\"><ns2:text>Hello World</ns2:text></ns2:greeting>";
        }
    }
}
