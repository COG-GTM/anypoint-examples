package org.mule.examples.springboot.extractingdatafromldapdirectory;

import org.springframework.ldap.core.DirContextAdapter;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.util.Base64;
import java.util.List;

/**
 * String form of the Mule LDAP connector's {@code LDAPEntry} as shown in the README: an LDIF-like block
 * ({@code dn: ...}, one {@code name: value} line per value, binary values as {@code name:: base64}) followed by a blank
 * line; a list of entries prints as {@code [entry, entry]}.
 */
final class LdapEntryFormatter {

    private LdapEntryFormatter() {
    }

    static String entry(DirContextAdapter context) throws NamingException {
        StringBuilder out = new StringBuilder("dn: ").append(context.getDn()).append('\n');
        NamingEnumeration<? extends Attribute> attributes = context.getAttributes().getAll();
        while (attributes.hasMore()) {
            Attribute attribute = attributes.next();
            NamingEnumeration<?> values = attribute.getAll();
            while (values.hasMore()) {
                Object value = values.next();
                if (value instanceof byte[] bytes) {
                    out.append(attribute.getID()).append(":: ").append(Base64.getEncoder().encodeToString(bytes));
                } else {
                    out.append(attribute.getID()).append(": ").append(value);
                }
                out.append('\n');
            }
        }
        return out.append('\n').toString();
    }

    /** {@code object-to-string} of the aggregated {@code List<LDAPEntry>}. */
    static String list(List<String> entries) {
        return entries.toString();
    }
}
