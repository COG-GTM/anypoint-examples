package org.mule.examples.springboot.documentintegrationusingthecmisconnector;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenCmisClientTest {

    @Test
    void usesTheAtomPubBindingOfTheMuleConfig() {
        var p = OpenCmisClient.sessionParameters("http://demo.openkm.com/OpenKM/cmis/atom", "u", "p", "default");
        assertThat(p).containsEntry(SessionParameter.BINDING_TYPE, "atompub")
                .containsEntry(SessionParameter.ATOMPUB_URL, "http://demo.openkm.com/OpenKM/cmis/atom")
                .containsEntry(SessionParameter.REPOSITORY_ID, "default")
                .containsEntry(SessionParameter.USER, "u")
                .containsEntry(SessionParameter.PASSWORD, "p");
    }
}
