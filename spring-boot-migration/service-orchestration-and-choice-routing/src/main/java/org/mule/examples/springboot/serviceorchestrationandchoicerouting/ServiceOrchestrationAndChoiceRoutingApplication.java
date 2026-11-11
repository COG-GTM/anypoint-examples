package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.cache.annotation.EnableCaching
@SpringBootApplication
public class ServiceOrchestrationAndChoiceRoutingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceOrchestrationAndChoiceRoutingApplication.class, args);
    }
}
