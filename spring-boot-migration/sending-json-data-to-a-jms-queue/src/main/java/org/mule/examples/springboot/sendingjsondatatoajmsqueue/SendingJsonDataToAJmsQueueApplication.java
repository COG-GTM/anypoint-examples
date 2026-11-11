package org.mule.examples.springboot.sendingjsondatatoajmsqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SendingJsonDataToAJmsQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(SendingJsonDataToAJmsQueueApplication.class, args);
    }
}
