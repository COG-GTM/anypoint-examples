package org.mule.examples.springboot.importinganemailattachmentusingtheimapconnector;

import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mail.dsl.Mail;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Flow {@code imap-to-csvFlow1}: {@code imaps:inbound-endpoint} (connector {@code checkFrequency="100"}) ->
 * expression-transformer {@code attachments-list} + {@code set-payload #[payload[0].getContent()]} -> DataWeave
 * "Map CSV to XML" -> logger -> {@code file:outbound-endpoint outputPattern="orders.xml"}.
 */
@Configuration(proxyBeanMethods = false)
class ImapToXmlFlowConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ImapToXmlFlowConfiguration.class);

    @Bean
    IntegrationFlow imapToXmlFlow(@Value("${imap.protocol}") String protocol,
                                  @Value("${imap.host}") String host,
                                  @Value("${imap.port}") int port,
                                  @Value("${imap.user}") String user,
                                  @Value("${imap.password}") String password,
                                  @Value("${imap.folder}") String folder,
                                  @Value("${imap.check-frequency}") long checkFrequency,
                                  @Value("${file.output-dir}") Path outputDir,
                                  @Value("${file.output-pattern}") String outputPattern) {
        String url = protocol + "://" + encode(user) + ":" + encode(password) + "@" + host + ":" + port + "/" + folder;
        return IntegrationFlow
                .from(Mail.imapInboundAdapter(url)
                                // Mule 3 mail connectors default to deleteReadMessages="true".
                                .shouldDeleteMessages(true)
                                .shouldMarkMessagesAsRead(true)
                                .autoCloseFolder(false),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(checkFrequency)).maxMessagesPerPoll(10)))
                .handle(MimeMessage.class, (message, headers) -> {
                    // The folder stays open until the attachment is read; closing it expunges the deleted message.
                    try (Closeable openFolder = headers.get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, Closeable.class)) {
                        String xml = OrdersCsvToXml.transform(firstAttachmentContent(message));
                        log.info("Received: {}", xml);
                        write(outputDir.resolve(outputPattern), xml);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return null;
                })
                .get();
    }

    private static String firstAttachmentContent(MimeMessage message) {
        try {
            Part attachment = MailAttachments.first(message);
            try (InputStream in = attachment.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Could not read the email attachment", e);
        }
    }

    private static void write(Path target, String xml) {
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, xml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
