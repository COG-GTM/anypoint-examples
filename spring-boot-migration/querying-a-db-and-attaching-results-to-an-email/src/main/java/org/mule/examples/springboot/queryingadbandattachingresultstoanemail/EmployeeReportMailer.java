package org.mule.examples.springboot.queryingadbandattachingresultstoanemail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * {@code set-attachment attachmentName="employees.csv" contentType="text/plain"} + {@code smtp:outbound-endpoint}
 * "Send an email": the Mule SMTP transport sends the payload as the text body and every outbound attachment as an
 * extra MIME part.
 */
@Component
class EmployeeReportMailer {

    private final JavaMailSender mailSender;
    private final String from;
    private final String to;
    private final String subject;

    EmployeeReportMailer(JavaMailSender mailSender,
                         @Value("${mail.from}") String from,
                         @Value("${mail.to}") String to,
                         @Value("${mail.subject}") String subject) {
        this.mailSender = mailSender;
        this.from = from;
        this.to = to;
        this.subject = subject;
    }

    void send(String csv) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(csv);
            helper.addAttachment("employees.csv", new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)), "text/plain");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not build the employees email", e);
        }
    }
}
