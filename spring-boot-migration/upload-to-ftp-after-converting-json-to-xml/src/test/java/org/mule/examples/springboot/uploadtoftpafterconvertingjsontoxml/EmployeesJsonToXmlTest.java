package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeesJsonToXmlTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void writesDataWeaveStyleDeclarationAndEscapesText() throws Exception {
        String xml = EmployeesJsonToXml.transform(mapper.readTree(
                "{\"employees\":{\"employee\":[{\"name\":\"A&B\",\"lastName\":\"<C>\",\"addresses\":{\"address\":[]}}]}}"));
        assertThat(xml).startsWith("<?xml version='1.0' encoding='UTF-8'?>\n<employees>")
                .contains("<name>A&amp;B</name>", "<lastName>&lt;C&gt;</lastName>", "<addresses></addresses>");
    }
}
