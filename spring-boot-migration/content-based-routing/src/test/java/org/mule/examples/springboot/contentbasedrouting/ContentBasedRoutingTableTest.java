package org.mule.examples.springboot.contentbasedrouting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;

/**
 * Table-driven routing parity: each row is the destination and reply the Mule {@code choice} in
 * content-based-routing.xml produces for a {@code language} value; the same value is fed through the
 * Spring Integration router.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ContentBasedRoutingTableTest {

    @Autowired
    GreetingGateway gateway;

    @ParameterizedTest(name = "language={0} -> {1}")
    @CsvSource(nullValues = "<absent>", value = {
            // language,  Mule destination,          reply,      language after routing
            "Spanish,     replyInSpanish,            Hola!,      Spanish",
            "French,      replyInFrench,             Bonjour!,   French",
            "<absent>,    replyInDefaultLanguage,    Hello!,     English",
            "English,     replyInDefaultLanguage,    Hello!,     English",
            "German,      replyInDefaultLanguage,    Hello!,     English",
            "spanish,     replyInDefaultLanguage,    Hello!,     English",
            "FRENCH,      replyInDefaultLanguage,    Hello!,     English",
            "'',          replyInDefaultLanguage,    Hello!,     English"
    })
    void routesLikeMuleChoice(String language, String muleDestination, String muleReply, String muleLanguageVar) {
        assertThat(LanguageRoutes.destination(language)).isEqualTo(muleDestination);

        Message<String> reply = gateway.greet(new GreetingRequest(language));
        assertThat(reply.getPayload()).isEqualTo(muleReply);
        assertThat(reply.getHeaders().get(ContentBasedRoutingFlowConfig.LANGUAGE_HEADER)).isEqualTo(muleLanguageVar);
    }
}
