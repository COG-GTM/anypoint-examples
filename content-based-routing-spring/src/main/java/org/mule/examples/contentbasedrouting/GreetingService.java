package org.mule.examples.contentbasedrouting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    private static final Logger logger = LoggerFactory.getLogger(GreetingService.class);

    public GreetingResponse getGreeting(String language) {
        String greeting;
        String resolvedLanguage;

        if (language == null || language.isBlank()) {
            logger.info("No language specified. Using English as a default.");
            resolvedLanguage = "English";
            greeting = "Hello!";
        } else {
            resolvedLanguage = language;
            greeting = switch (language) {
                case "Spanish" -> "Hola!";
                case "French" -> "Bonjour!";
                default -> {
                    logger.info("No language specified. Using English as a default.");
                    resolvedLanguage = "English";
                    yield "Hello!";
                }
            };
        }

        logger.info("The reply \"{}\" means \"hello\" in {}.", greeting, resolvedLanguage);
        return new GreetingResponse(greeting, resolvedLanguage);
    }

    public record GreetingResponse(String greeting, String language) {}
}
