package com.lifeos.ai;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "lifeos.ai.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiDisabledIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void generateSuggestionsFailsWhenAiDisabled() throws Exception {
        MvcResult reg = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "noai@example.com", "password", "password123", "displayName", "No AI"))))
                .andExpect(status().isCreated()).andReturn();
        MockHttpSession session = (MockHttpSession) reg.getRequest().getSession(false);

        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        MvcResult upload = mockMvc.perform(multipart("/api/v1/documents").file(file).session(session).with(csrf()))
                .andExpect(status().isCreated()).andReturn();
        UUID docId = UUID.fromString(objectMapper.readTree(upload.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/documents/" + docId + "/suggestions").session(session).with(csrf()))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("AI_DISABLED"));
    }
}
