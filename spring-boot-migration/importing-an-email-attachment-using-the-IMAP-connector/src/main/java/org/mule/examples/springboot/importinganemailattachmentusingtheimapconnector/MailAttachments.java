package org.mule.examples.springboot.importinganemailattachmentusingtheimapconnector;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeUtility;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mule 3 mail transport's view of a message's attachments ({@code MailUtils.getAttachments}): every leaf part of a
 * multipart message, keyed by its description, else its decoded file name, else a running counter, in key order.
 * {@code #[payload[0]]} after the {@code attachments-list} evaluator is the first entry.
 */
final class MailAttachments {

    private MailAttachments() {
    }

    static Map<String, Part> of(Part message) throws MessagingException, IOException {
        Map<String, Part> attachments = new TreeMap<>();
        if (message.getContent() instanceof Multipart multipart) {
            collect(multipart, attachments, new int[] {0});
        }
        return attachments;
    }

    static Part first(Part message) throws MessagingException, IOException {
        Map<String, Part> attachments = of(message);
        if (attachments.isEmpty()) {
            throw new IllegalStateException("Message has no attachments");
        }
        return attachments.values().iterator().next();
    }

    private static void collect(Multipart multipart, Map<String, Part> attachments, int[] counter)
            throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);
            if (part.isMimeType("multipart/*") && part.getContent() instanceof Multipart nested) {
                collect(nested, attachments, counter);
                continue;
            }
            String key;
            if (part.getDescription() != null && !part.getDescription().isEmpty()) {
                key = part.getDescription();
            } else if (part.getFileName() != null && !part.getFileName().isEmpty()) {
                key = decode(part.getFileName());
            } else {
                key = String.valueOf(counter[0]++);
            }
            attachments.put(key, part);
        }
    }

    private static String decode(String fileName) {
        try {
            return MimeUtility.decodeText(fileName);
        } catch (UnsupportedEncodingException e) {
            return fileName;
        }
    }
}
