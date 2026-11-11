package org.mule.examples.springboot.scattergatherflowcontrol;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;

/**
 * HTTP parity for {@code /scatterGather} plus the SMTP side effect, captured by a local GreenMail server standing in
 * for the Gmail connector. Each fixture's {@code expectedEmail} holds the e-mail the Mule flow sends.
 */
class ScatterGatherFlowControlParityTest extends AbstractWireMockParityTest {

    private static final String FIXTURES = "parity/scatter-gather-flow-control";
    static final GreenMail SMTP = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

    static {
        SMTP.start();
    }

    @DynamicPropertySource
    static void smtp(DynamicPropertyRegistry registry) {
        registry.add("smtp.host", () -> "127.0.0.1");
        registry.add("smtp.port", () -> SMTP.getSmtp().getPort());
        registry.add("smtp.protocol", () -> "smtp");
        registry.add("smtp.auth", () -> "false");
        registry.add("mail.from", () -> "mule-example@example.com");
        registry.add("mail.to", () -> "report-recipient@example.com");
    }

    @Override
    protected String fixtureDirectory() {
        return FIXTURES;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        super.beforeCase(fixture);
        try {
            SMTP.purgeEmailFromAllMailboxes();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        super.afterCase(fixture, spring);
        JsonNode expected = rawFixture(fixture.name()).path("expectedEmail");
        try {
            MimeMessage[] received = SMTP.getReceivedMessages();
            if (expected.isMissingNode() || expected.isNull()) {
                assertThat(received).as("[%s] e-mails sent", fixture.name()).isEmpty();
                return;
            }
            assertThat(received).as("[%s] e-mails sent", fixture.name()).hasSize(1);
            MimeMessage mail = received[0];
            assertThat(mail.getSubject()).isEqualTo(expected.path("subject").asText());
            assertThat(mail.getFrom()[0].toString()).isEqualTo(expected.path("from").asText());
            assertThat(mail.getAllRecipients()[0].toString()).isEqualTo(expected.path("to").asText());
            Multipart parts = (Multipart) mail.getContent();
            String text = null;
            BodyPart attachment = null;
            for (int i = 0; i < parts.getCount(); i++) {
                BodyPart part = parts.getBodyPart(i);
                if (part.getFileName() != null) {
                    attachment = part;
                } else if (part.getContent() instanceof Multipart nested) {
                    text = nested.getBodyPart(0).getContent().toString();
                } else {
                    text = part.getContent().toString();
                }
            }
            assertThat(text).isEqualTo(expected.path("body").asText());
            JsonNode att = expected.path("attachment");
            assertThat(attachment).as("attachment").isNotNull();
            assertThat(attachment.getFileName()).isEqualTo(att.path("name").asText());
            assertThat(attachment.getContentType()).startsWith(att.path("contentType").asText());
            assertThat(new String(attachment.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    // SMTP transports text parts with canonical CRLF line endings, for Mule and Spring alike.
                    .replace("\r\n", "\n"))
                    .isEqualTo(att.path("content").asText());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    static JsonNode rawFixture(String name) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath:" + FIXTURES + "/*.json")) {
                JsonNode node = mapper.readTree(r.getInputStream());
                if (name.equals(node.path("name").asText())) {
                    return node;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new IllegalStateException("No fixture named " + name);
    }
}
