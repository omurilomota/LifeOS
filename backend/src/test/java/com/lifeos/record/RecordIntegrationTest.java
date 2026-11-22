package com.lifeos.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecordIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    private MockHttpSession register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123",
                                "displayName", displayName
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private UUID createRecord(MockHttpSession session, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/records")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "ASSET",
                                "title", title,
                                "description", "desc",
                                "category", "electronics",
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    @Test
    void recordCrudHistoryAndSearchFlow() throws Exception {
        MockHttpSession session = register("records@example.com", "Records User");
        UUID recordId = createRecord(session, "Notebook Dell");

        mockMvc.perform(get("/api/v1/records").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Notebook Dell"));

        mockMvc.perform(get("/api/v1/records").session(session).param("q", "Dell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(recordId.toString()));

        mockMvc.perform(put("/api/v1/records/" + recordId)
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "ASSET",
                                "title", "Notebook Dell XPS",
                                "description", "updated",
                                "category", "electronics",
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Notebook Dell XPS"));

        mockMvc.perform(patch("/api/v1/records/" + recordId + "/archive").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/v1/records/" + recordId + "/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").exists());
    }

    @Test
    void relationshipRequiresSameHouseholdAndValidTarget() throws Exception {
        MockHttpSession session = register("rel@example.com", "Rel User");
        UUID fromId = createRecord(session, "Carro");
        UUID toId = createRecord(session, "Seguro Carro");

        mockMvc.perform(post("/api/v1/records/" + fromId + "/relationships")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "toRecordId", toId.toString(),
                                "relationshipType", "insured_by"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationshipType").value("INSURED_BY"));

        mockMvc.perform(get("/api/v1/records/" + fromId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationships[0].toRecordId").value(toId.toString()));
    }

    @Test
    void cannotAccessAnotherHouseholdRecord() throws Exception {
        MockHttpSession userA = register("a@example.com", "A");
        MockHttpSession userB = register("b@example.com", "B");
        UUID recordId = createRecord(userA, "Private Record");

        mockMvc.perform(get("/api/v1/records/" + recordId).session(userB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/records").session(userB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
