package org.mule.examples.springboot.queryingadbandattachingresultstoanemail;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityContainers;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * HTTP parity ({@code POST /} with the employee XML) against MySQL 5.7 seeded like the Mule IT, plus the email side
 * effect checked in GreenMail: {@code expectedEmail} (headers + text body) and {@code expectedAttachment}
 * ({@code File-Name}, {@code Content-Type}, content).
 */
@Testcontainers
class AttachmentsParityTest extends AbstractParityTest {

    static final String FIXTURES = "parity/querying-a-db-and-attaching-results-to-an-email";

    @Container
    static final MySQLContainer<?> MYSQL = ParityContainers.mysql();

    @Container
    static final GenericContainer<?> GREENMAIL = ParityContainers.greenMail();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jdbc.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("smtp.host", GREENMAIL::getHost);
        registry.add("smtp.port", () -> GREENMAIL.getMappedPort(3025));
    }

    private final GreenMailInbox inbox = new GreenMailInbox(GREENMAIL, "receiver@localhost");

    @Override
    protected String fixtureDirectory() {
        return FIXTURES;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        try {
            inbox.purge();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        JsonNode raw = FixtureExtras.raw(FIXTURES, fixture.name());
        try {
            await().atMost(Duration.ofSeconds(20)).until(() -> !inbox.messages().isEmpty());
            List<MimeMessage> received = inbox.messages();
            assertThat(received).as("[%s] emails sent", fixture.name()).hasSize(1);
            MimeMessage email = received.get(0);
            Multipart parts = (Multipart) email.getContent();
            Part text = null;
            Part attachment = null;
            for (int i = 0; i < parts.getCount(); i++) {
                Part p = parts.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(p.getDisposition()) || p.getFileName() != null) {
                    attachment = p;
                } else if (text == null) {
                    text = p;
                }
            }
            assertThat(text).as("text part").isNotNull();
            assertThat(attachment).as("attachment part").isNotNull();
            FixtureExtras.assertSideEffect(fixture, "email", raw.path("expectedEmail"), Map.of(
                    "Subject", List.of(email.getSubject()),
                    "From", List.of(GreenMailInbox.addresses(email.getFrom())),
                    "To", List.of(GreenMailInbox.addresses(email.getRecipients(Message.RecipientType.TO))),
                    "Content-Type", List.of(email.getContentType())), bytes(text));
            FixtureExtras.assertSideEffect(fixture, "attachment", raw.path("expectedAttachment"), Map.of(
                    "File-Name", List.of(attachment.getFileName()),
                    "Content-Type", List.of(attachment.getContentType())), bytes(attachment));
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("[" + fixture.name() + "] email not readable: " + e, e);
        }
    }

    private static byte[] bytes(Part part) throws Exception {
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
