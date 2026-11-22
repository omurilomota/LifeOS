package com.lifeos.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void propagatesSanitizedRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Request-ID", "e2e-run-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "e2e-run-123"));

        mockMvc.perform(get("/api/v1/health").header("X-Request-ID", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "scriptalert1script"));
    }

    @Test
    void exposesPrometheusMetrics() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("jvm_");
    }
}
