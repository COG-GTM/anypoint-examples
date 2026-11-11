package org.mule.examples.springboot.importinganemailattachmentusingthepop3connector;

import jakarta.activation.DataHandler;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;

/**
 * Email-triggered parity against GreenMail. Like the Mule IT, each fixture's {@code request.body} is sent over SMTP
 * to the polled mailbox as a single attachment ({@code Attachment-Name}, {@code Attachment-Content-Type}).
 * {@code expected} describes the side effects: the XML written to {@code orders.xml} (body), and the output file
 * name and the messages left in the mailbox (headers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.import=optional:classpath:/parity/parity-defaults.yml")
@ActiveProfiles("parity")
@Testcontainers
class PopToXmlParityTest {

    static final String FIXTURES = "parity/importing-an-email-attachment-using-the-POP3-connector";
    static final String MAILBOX = "parity@localhost";
    static final Path OUTPUT_DIR = tempDir();

    @Container
    static final GenericContainer<?> GREENMAIL = ParityContainers.greenMail();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("pop3.protocol", () -> "pop3");
        registry.add("pop3.host", GREENMAIL::getHost);
        registry.add("pop3.port", () -> GREENMAIL.getMappedPort(3110));
        registry.add("pop3.user", () -> MAILBOX);
        registry.add("pop3.password", () -> "parity");
        registry.add("file.output-dir", OUTPUT_DIR::toString);
    }

    @TestFactory
    Stream<DynamicTest> parity() {
        return ParityFixtures.load(FIXTURES).stream().map(f -> DynamicTest.dynamicTest(f.name(), () -> runCase(f)));
    }

    private void runCase(ParityFixture fixture) throws Exception {
        Path output = OUTPUT_DIR.resolve("orders.xml");
        Files.deleteIfExists(output);
        send(fixture);

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(100))
                .until(() -> Files.exists(output) && messagesInInbox() == 0);

        Map<String, List<String>> facts = Map.of(
                "File-Name", List.of(output.getFileName().toString()),
                "Messages-Left-In-Inbox", List.of(String.valueOf(messagesInInbox())));
        FixtureExtras.assertSideEffect(fixture, "orders.xml", FixtureExtras.raw(FIXTURES, fixture.name()).path("expected"),
                facts, Files.readAllBytes(output));
    }

    private static void send(ParityFixture fixture) throws Exception {
        Map<String, String> h = fixture.request().headers();
        Properties props = new Properties();
        props.put("mail.smtp.host", GREENMAIL.getHost());
        props.put("mail.smtp.port", String.valueOf(GREENMAIL.getMappedPort(3025)));
        MimeMessage message = new MimeMessage(Session.getInstance(props));
        message.setFrom(new InternetAddress(h.get("From")));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(MAILBOX));
        message.setSubject(h.get("Subject"));
        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                ParityFixtures.bodyText(fixture.request().body()).getBytes(StandardCharsets.UTF_8),
                h.get("Attachment-Content-Type"))));
        attachment.setFileName(h.get("Attachment-Name"));
        message.setContent(new MimeMultipart(attachment));
        Transport.send(message);
    }

    private static int messagesInInbox() throws Exception {
        Session session = Session.getInstance(new Properties());
        try (Store store = session.getStore("pop3")) {
            store.connect(GREENMAIL.getHost(), GREENMAIL.getMappedPort(3110), MAILBOX, "parity");
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            try {
                return (int) Stream.of(inbox.getMessages())
                        .filter(m -> {
                            try {
                                return !m.isSet(jakarta.mail.Flags.Flag.DELETED);
                            } catch (jakarta.mail.MessagingException e) {
                                return true;
                            }
                        }).count();
            } finally {
                inbox.close(false);
            }
        }
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("pop3-output");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
