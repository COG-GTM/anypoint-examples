package org.mule.examples.springboot.scattergatherflowcontrol;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;

/**
 * {@code outboundFlow}: set-attachment "aggregation-report" (text/plain) → set-payload → smtp:outbound-endpoint
 * (Gmail connector). The message body is the payload at send time.
 */
@Component
public class AggregationReportMailer {

    static final String BODY = "Please find attached your Aggregation Report";
    static final String SUBJECT = "Scatter gather example - aggregation";
    static final String ATTACHMENT = "aggregation-report";

    private final JavaMailSender mailSender;
    private final String from;
    private final String to;

    public AggregationReportMailer(JavaMailSender mailSender,
                                   @Value("${mail.from:sender@example.com}") String from,
                                   @Value("${mail.to:recipient@example.com}") String to) {
        this.mailSender = mailSender;
        this.from = from;
        this.to = to;
    }

    public String send(String report) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(SUBJECT);
            helper.setText(BODY);
            helper.addAttachment(ATTACHMENT, new ByteArrayResource(report.getBytes(StandardCharsets.UTF_8)), "text/plain");
            mailSender.send(message);
            return BODY;
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
