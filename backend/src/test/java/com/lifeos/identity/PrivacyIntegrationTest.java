package com.lifeos.identity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivacyIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void exportAndDeleteAccount() throws Exception {
        MvcResult reg = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "privacy@example.com", "password", "password123", "displayName", "Privacy"))))
                .andExpect(status().isCreated()).andReturn();
        MockHttpSession session = (MockHttpSession) reg.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/privacy/export").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("privacy@example.com"))
                .andExpect(jsonPath("$.household.name").exists());

        mockMvc.perform(delete("/api/v1/privacy/account").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
