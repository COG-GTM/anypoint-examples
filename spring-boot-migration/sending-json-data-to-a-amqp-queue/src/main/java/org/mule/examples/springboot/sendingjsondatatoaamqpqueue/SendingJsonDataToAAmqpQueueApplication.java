package org.mule.examples.springboot.sendingjsondatatoaamqpqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SendingJsonDataToAAmqpQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(SendingJsonDataToAAmqpQueueApplication.class, args);
    }
}
