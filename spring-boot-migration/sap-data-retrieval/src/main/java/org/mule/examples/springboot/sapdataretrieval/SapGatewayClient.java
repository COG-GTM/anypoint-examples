package org.mule.examples.springboot.sapdataretrieval;

import static org.mule.examples.springboot.sapdataretrieval.BapiXml.fields;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link SapClient} over HTTP: POSTs the connector's BAPI XML document to {@code /functions/{functionName}} on an SAP
 * gateway (Basic auth with the JCo user, {@code sap-client}/{@code sap-language} headers) and reads the result table
 * from the XML reply.
 */
@Component
public class SapGatewayClient implements SapClient {

    private final WebClient webClient;

    public SapGatewayClient(WebClient.Builder builder, SapProperties properties) {
        SapProperties.Jco jco = properties.jco();
        this.webClient = builder
                .baseUrl(properties.gatewayBaseUrl())
                .defaultHeaders(h -> {
                    h.setBasicAuth(jco.user(), jco.passwd());
                    h.set("sap-client", jco.client());
                    h.set("sap-language", jco.lang());
                })
                .build();
    }

    @Override
    public List<SapRow> findCustomers(SapRequests.CustomerFind r) {
        String xml = BapiXml.write("BAPI_CUSTOMER_FIND",
                fields("MAX_CNT", r.maxCount(), "PL_HOLD", "X"),
                "SELOPT_TAB",
                fields("COMP_CODE", "0001", "TABNAME", "KNA1", "FIELDNAME", "NAME1", "FIELDVALUE", r.namePattern()));
        return rows(execute("BAPI_CUSTOMER_FIND", xml), "RESULT_TAB");
    }

    @Override
    public List<SapRow> getMaterials(SapRequests.MaterialGetList r) {
        String xml = BapiXml.write("BAPI_MATERIAL_GETLIST",
                fields("MAXROWS", r.maxRows()),
                "MATERIALSHORTDESCSEL",
                fields("SIGN", "I", "OPTION", "CP", "DESCR_LOW", r.descriptionPattern()));
        return rows(execute("BAPI_MATERIAL_GETLIST", xml), "MATNRLIST");
    }

    @Override
    public List<SapRow> getSalesOrders(SapRequests.SalesOrderGetList r) {
        String xml = BapiXml.write("BAPI_SALESORDER_GETLIST",
                fields("CUSTOMER_NUMBER", r.customerNumber(), "SALES_ORGANIZATION", r.salesOrganization()),
                null, Map.of());
        return rows(execute("BAPI_SALESORDER_GETLIST", xml), "SALES_ORDERS");
    }

    private ResponseEntity<byte[]> execute(String functionName, String requestXml) {
        return webClient.post()
                .uri("/functions/{functionName}", functionName)
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .bodyValue(requestXml)
                .retrieve()
                .toEntity(byte[].class)
                .block();
    }

    private static List<SapRow> rows(ResponseEntity<byte[]> reply, String table) {
        MediaType contentType = reply.getHeaders().getContentType();
        Charset charset = contentType == null ? null : contentType.getCharset();
        return BapiXml.rows(reply.getBody() == null ? new byte[0] : reply.getBody(), charset, table);
    }
}
