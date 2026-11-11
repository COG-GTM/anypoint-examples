package org.mule.examples.springboot.contentbasedrouting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/** Spring Integration equivalent of {@code content-based-routingFlow} and the {@code replyInDefaultLanguage} sub-flow. */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class ContentBasedRoutingFlowConfig {

    public static final String INPUT = "contentBasedRoutingFlow.input";
    public static final String LANGUAGE_HEADER = "language";

    private static final Logger log = LoggerFactory.getLogger(ContentBasedRoutingFlowConfig.class);

    @Bean
    public IntegrationFlow contentBasedRoutingFlow() {
        return f -> f
                .enrichHeaders(h -> h.headerFunction(LANGUAGE_HEADER,
                        (Message<GreetingRequest> m) -> m.getPayload().language()))
                .<GreetingRequest, String>route(r -> LanguageRoutes.destination(r.language()), r -> r
                        .subFlowMapping(LanguageRoutes.SPANISH, sf -> sf.transform(p -> "Hola!"))
                        .subFlowMapping(LanguageRoutes.FRENCH, sf -> sf.transform(p -> "Bonjour!"))
                        .subFlowMapping(LanguageRoutes.DEFAULT, sf -> sf.handle((p, h) -> {
                            log.info("No language specified. Using English as a default. ");
                            return MessageBuilder.withPayload("Hello!").setHeader(LANGUAGE_HEADER, "English").build();
                        })))
                .handle((p, h) -> {
                    log.info("The reply \"{}\" means \"hello\" in {}.", p, h.get(LANGUAGE_HEADER));
                    return p;
                });
    }
}
