package org.mule.examples.contentbasedrouting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnHolaForSpanish() throws Exception {
        mockMvc.perform(get("/").param("language", "Spanish"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hola!"));
    }

    @Test
    void shouldReturnBonjourForFrench() throws Exception {
        mockMvc.perform(get("/").param("language", "French"))
                .andExpect(status().isOk())
                .andExpect(content().string("Bonjour!"));
    }

    @Test
    void shouldReturnHelloForNoLanguage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello!"));
    }

    @Test
    void shouldReturnHelloForUnknownLanguage() throws Exception {
        mockMvc.perform(get("/").param("language", "German"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello!"));
    }
}
