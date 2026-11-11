package org.mule.examples.springboot.importinganemailattachmentusingtheimapconnector;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailAttachmentsTest {

    @Test
    void keysPartsLikeMuleMailUtils() throws Exception {
        MimeBodyPart body = new MimeBodyPart();
        body.setText("hello");
        MimeBodyPart b = new MimeBodyPart();
        b.setText("b");
        b.setFileName("b.csv");
        MimeBodyPart a = new MimeBodyPart();
        a.setText("a");
        a.setFileName("a.csv");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setContent(new MimeMultipart(body, b, a));
        message.saveChanges();

        assertThat(MailAttachments.of(message).keySet()).containsExactly("0", "a.csv", "b.csv");
    }

    @Test
    void plainTextMessageHasNoAttachments() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setText("no attachments");
        message.saveChanges();
        assertThatThrownBy(() -> MailAttachments.first(message)).isInstanceOf(IllegalStateException.class);
    }
}
