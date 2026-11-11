package org.mule.examples.springboot.sendingacsvfilethroughemailusingsmtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.integration.mail.dsl.MailSendingMessageHandlerSpec;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Flow {@code csv-to-smtpFlow}: {@code file:inbound-endpoint "Poll files"} -> DataWeave "Transform CSV to Map" ->
 * logger "Processing objects: #[payload]" -> {@code smtp:outbound-endpoint} (Gmail connector, STARTTLS on 587). The
 * Mule SMTP transport sends the payload's string form as a text/plain body.
 */
@Configuration(proxyBeanMethods = false)
class CsvToSmtpFlowConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CsvToSmtpFlowConfiguration.class);

    @Bean
    IntegrationFlow csvToSmtpFlow(@Value("${file.input-dir}") File inputDir,
                                  @Value("${file.polling-frequency}") long pollingFrequency,
                                  @Value("${smtp.host}") String host,
                                  @Value("${smtp.port}") int port,
                                  @Value("${smtp.user}") String user,
                                  @Value("${smtp.password}") String password,
                                  @Value("${smtp.starttls}") boolean starttls,
                                  @Value("${mail.from}") String from,
                                  @Value("${mail.to}") String to,
                                  @Value("${mail.subject}") String subject) {
        MailSendingMessageHandlerSpec smtp = Mail.outboundAdapter(host).port(port).protocol("smtp")
                .javaMailProperties(p -> p
                        .put("mail.smtp.auth", String.valueOf(!user.isBlank()))
                        .put("mail.smtp.starttls.enable", String.valueOf(starttls))
                        .put("mail.smtp.starttls.required", String.valueOf(starttls)));
        if (!user.isBlank()) {
            smtp.credentials(user, password);
        }
        return IntegrationFlow
                .from(Files.inboundAdapter(inputDir).autoCreateDirectory(true).preventDuplicates(false).ignoreHidden(true),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(pollingFrequency))))
                .handle(File.class, (file, headers) -> {
                    String csv = read(file);
                    // Mule 3 file connector default autoDelete="true".
                    file.delete();
                    return csv;
                })
                .<String, String>transform(csv -> {
                    List<Map<String, Object>> orders = OrdersCsv.parse(csv);
                    log.info("Processing objects: {}", orders);
                    return orders.toString();
                })
                .enrichHeaders(h -> h
                        .header(MailHeaders.FROM, from)
                        .header(MailHeaders.TO, to)
                        .header(MailHeaders.SUBJECT, subject))
                .handle(smtp)
                .get();
    }

    private static String read(File file) {
        try {
            return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
