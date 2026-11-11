package org.mule.examples.springboot.queryingadbandattachingresultstoanemail;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import org.testcontainers.containers.GenericContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reads (and empties) a GreenMail mailbox over IMAP; messages are copied so they stay usable after the folder closes. */
final class GreenMailInbox {

    private final GenericContainer<?> greenMail;
    private final String user;

    GreenMailInbox(GenericContainer<?> greenMail, String user) {
        this.greenMail = greenMail;
        this.user = user;
    }

    List<MimeMessage> messages() throws Exception {
        return withInbox(false);
    }

    void purge() throws Exception {
        withInbox(true);
    }

    static String addresses(Address[] addresses) {
        return addresses == null ? "" : Stream.of(addresses).map(Address::toString).collect(Collectors.joining(", "));
    }

    private List<MimeMessage> withInbox(boolean delete) throws Exception {
        Session session = Session.getInstance(new Properties());
        try (Store store = session.getStore("imap")) {
            store.connect(greenMail.getHost(), greenMail.getMappedPort(3143), user, "parity");
            Folder inbox = store.getFolder("INBOX");
            inbox.open(delete ? Folder.READ_WRITE : Folder.READ_ONLY);
            List<MimeMessage> copies = new ArrayList<>();
            for (Message m : inbox.getMessages()) {
                ByteArrayOutputStream raw = new ByteArrayOutputStream();
                m.writeTo(raw);
                copies.add(new MimeMessage(session, new ByteArrayInputStream(raw.toByteArray())));
                if (delete) {
                    m.setFlag(Flags.Flag.DELETED, true);
                }
            }
            inbox.close(delete);
            return copies;
        }
    }
}
