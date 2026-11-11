package org.mule.examples.springboot.extractingdatafromldapdirectory;

import org.junit.jupiter.api.Test;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.support.LdapNameBuilder;

import javax.naming.directory.BasicAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class LdapEntryFormatterTest {

    @Test
    void printsBinaryValuesAsBase64AndEndsWithBlankLine() throws Exception {
        BasicAttributes attributes = new BasicAttributes(true);
        attributes.put("uid", "mmc");
        attributes.put("userPassword", "mmc123".getBytes());
        DirContextAdapter ctx = new DirContextAdapter(attributes, LdapNameBuilder.newInstance("ou=people").add("cn", "mmc").build());

        String entry = LdapEntryFormatter.entry(ctx);

        assertThat(entry).startsWith("dn: cn=mmc,ou=people\n").contains("uid: mmc\n", "userPassword:: bW1jMTIz\n").endsWith("\n\n");
        assertThat(LdapEntryFormatter.list(java.util.List.of(entry, entry))).startsWith("[dn: ").contains("\n\n, dn: ").endsWith("\n\n]");
    }
}
