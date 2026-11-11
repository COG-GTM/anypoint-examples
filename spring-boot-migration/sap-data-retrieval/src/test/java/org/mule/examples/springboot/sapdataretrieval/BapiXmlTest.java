package org.mule.examples.springboot.sapdataretrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.List;
import org.junit.jupiter.api.Test;

class BapiXmlTest {

    @Test
    void decodesRepliesUsingTheEncodingDeclaredInTheXml() {
        byte[] reply = ("<?xml version='1.0' encoding='windows-1252'?><BAPI_CUSTOMER_FIND><tables><RESULT_TAB>"
                + "<row><FIELDVALUE>M\u00fcller Caf\u00e9</FIELDVALUE></row></RESULT_TAB></tables></BAPI_CUSTOMER_FIND>")
                .getBytes(Charset.forName("windows-1252"));

        List<SapRow> rows = BapiXml.rows(reply, "RESULT_TAB");

        assertThat(rows).singleElement().satisfies(r ->
                assertThat(r.fields()).containsEntry("FIELDVALUE", "M\u00fcller Caf\u00e9"));
    }
}
