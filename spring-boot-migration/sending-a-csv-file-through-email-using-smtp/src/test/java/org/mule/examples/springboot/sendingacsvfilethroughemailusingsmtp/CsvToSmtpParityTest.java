package org.mule.examples.springboot.sendingacsvfilethroughemailusingsmtp;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mule.examples.springboot.parity.ParityContainers;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * File-triggered parity against GreenMail: {@code request.body} is dropped into the input directory as
 * {@code request.headers.File-Name}; {@code expected} describes the email that arrives in {@code mail.to}'s mailbox
 * (Subject/From/To/Content-Type headers, text body compared after trimming like the Mule IT) plus
 * {@code Input-File-Deleted}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
@Testcontainers
class CsvToSmtpParityTest {

    static final String FIXTURES = "parity/sending-a-csv-file-through-email-using-smtp";
    static final String MAILBOX = "receiver@localhost";
    static final Path INPUT_DIR = tempDir("smtp-input");
    static final Path STAGING_DIR = tempDir("smtp-staging");

    @Container
    static final GenericContainer<?> GREENMAIL = ParityContainers.greenMail();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("smtp.host", GREENMAIL::getHost);
        registry.add("smtp.port", () -> GREENMAIL.getMappedPort(3025));
        registry.add("file.input-dir", INPUT_DIR::toString);
    }

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load(FIXTURES).stream().map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    private void runCase(ParityFixture fixture) throws Exception {
        GreenMailInbox inbox = new GreenMailInbox(GREENMAIL, MAILBOX);
        inbox.purge();
        String fileName = fixture.request().headers().get("File-Name");
        Path staged = STAGING_DIR.resolve(fileName);
        Files.writeString(staged, ParityFixtures.bodyText(fixture.request().body()), StandardCharsets.UTF_8);
        Path input = INPUT_DIR.resolve(fileName);
        Files.move(staged, input, StandardCopyOption.ATOMIC_MOVE);

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).until(() -> !inbox.messages().isEmpty());
        Thread.sleep(300);
        List<MimeMessage> received = inbox.messages();
        assertThat(received).as("[%s] emails sent", fixture.name()).hasSize(1);
        MimeMessage email = received.get(0);
        Map<String, List<String>> facts = Map.of(
                "Subject", List.of(email.getSubject()),
                "From", List.of(GreenMailInbox.addresses(email.getFrom())),
                "To", List.of(GreenMailInbox.addresses(email.getRecipients(Message.RecipientType.TO))),
                "Content-Type", List.of(email.getContentType()),
                "Input-File-Deleted", List.of(String.valueOf(!Files.exists(input))));
        String body = email.getContent().toString().trim();
        FixtureExtras.assertSideEffect(fixture, "email", FixtureExtras.raw(FIXTURES, fixture.name()).path("expected"),
                facts, body.getBytes(StandardCharsets.UTF_8));
    }

    private static Path tempDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
