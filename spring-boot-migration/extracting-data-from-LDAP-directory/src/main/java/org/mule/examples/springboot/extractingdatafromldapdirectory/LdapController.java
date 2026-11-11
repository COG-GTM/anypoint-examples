package org.mule.examples.springboot.extractingdatafromldapdirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.SearchScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

/**
 * Flow {@code ldapFlow1}: {@code http:listener path="/"} -> {@code ldap:search baseDn="ou=people"
 * filter="(objectClass=*)"} (connector default scope: one level) -> collection-splitter/logger/collection-aggregator
 * -> {@code object-to-string}.
 */
@RestController
class LdapController {

    private static final Logger log = LoggerFactory.getLogger(LdapController.class);
    private static final LdapQuery PEOPLE = query()
            .base("ou=people")
            .searchScope(SearchScope.ONELEVEL)
            .filter("(objectClass=*)");

    private final LdapTemplate ldap;

    LdapController(LdapTemplate ldap) {
        this.ldap = ldap;
    }

    @RequestMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    String searchPeople() {
        List<String> users = ldap.search(PEOPLE, (ContextMapper<String>) ctx -> LdapEntryFormatter.entry((DirContextAdapter) ctx));
        users.forEach(user -> log.info("LDAP user: {}", user));
        String all = LdapEntryFormatter.list(users);
        log.info("all users: {}", all);
        return all;
    }
}
