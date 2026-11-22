package com.lifeos.ai;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "lifeos.ai.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiSuggestionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private MockHttpSession register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123", "displayName", name))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private UUID uploadTxt(MockHttpSession session) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contrato.txt", "text/plain", "conteudo".getBytes());
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents").file(file).session(session).with(csrf()))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void generateListConfirmAndRejectSuggestions() throws Exception {
        MockHttpSession session = register("ai@example.com", "AI User");
        UUID docId = uploadTxt(session);

        MvcResult gen = mockMvc.perform(post("/api/v1/documents/" + docId + "/suggestions").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();
        JsonNode suggestions = objectMapper.readTree(gen.getResponse().getContentAsString());
        UUID firstId = UUID.fromString(suggestions.get(0).get("id").asText());
        UUID secondId = UUID.fromString(suggestions.get(1).get("id").asText());

        mockMvc.perform(get("/api/v1/documents/" + docId + "/suggestions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/api/v1/ai/suggestions/" + firstId + "/confirm").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdEntityId").exists());

        mockMvc.perform(post("/api/v1/ai/suggestions/" + secondId + "/reject").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
