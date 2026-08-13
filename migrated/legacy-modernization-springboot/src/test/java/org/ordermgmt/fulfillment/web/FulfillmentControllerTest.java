package org.ordermgmt.fulfillment.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ordermgmt.fulfillment.SoapFixtures;
import org.ordermgmt.fulfillment.model.ShippingOrder;
import org.ordermgmt.fulfillment.service.LegacyFulfillmentDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FulfillmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LegacyFulfillmentDispatcher dispatcher;

    @Test
    void acknowledgesAnOrderWithTheSameSoapResponseAsTheMuleFlow() throws Exception {
        MvcResult result = mockMvc.perform(post("/OrderFulfillment")
                        .contentType(MediaType.TEXT_XML)
                        .content(SoapFixtures.REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo(SoapFixtures.EXPECTED_REPLY);
    }

    @Test
    void handsTheOrderToTheLegacyFulfillmentDispatcher() throws Exception {
        mockMvc.perform(post("/OrderFulfillment").contentType(MediaType.TEXT_XML).content(SoapFixtures.REQUEST))
                .andExpect(status().isOk());

        ArgumentCaptor<ShippingOrder> captured = ArgumentCaptor.forClass(ShippingOrder.class);
        verify(dispatcher).dispatch(captured.capture());
        assertThat(captured.getValue().shippingId()).isEqualTo("1234");
    }

    @Test
    void returnsAClientSoapFaultForMalformedXml() throws Exception {
        mockMvc.perform(post("/OrderFulfillment").contentType(MediaType.TEXT_XML).content("<soapenv:Envelope>"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<faultcode>soap:Client</faultcode>")));
    }

    @Test
    void returnsAServerSoapFaultWhenOrderProcessingFails() throws Exception {
        doThrow(new IllegalStateException("boom")).when(dispatcher).dispatch(any());

        mockMvc.perform(post("/OrderFulfillment").contentType(MediaType.TEXT_XML).content(SoapFixtures.REQUEST))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<faultcode>soap:Server</faultcode>")));
    }

    @Test
    void rejectsMethodsOtherThanPostJustLikeTheMuleListener() throws Exception {
        mockMvc.perform(get("/OrderFulfillment"))
                .andExpect(status().isMethodNotAllowed());
    }
}
