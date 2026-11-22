package com.lifeos.obligation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class ObligationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private MockHttpSession register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123", "displayName", name))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private UUID createObligation(MockHttpSession session, String title, Instant dueAt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/obligations").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "desc",
                                "dueAt", dueAt.toString(),
                                "priority", "HIGH",
                                "status", "OPEN"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    @Test
    void createCompleteAndDashboardFlow() throws Exception {
        MockHttpSession session = register("obl@example.com", "Obl User");
        Instant dueSoon = Instant.now().plus(2, ChronoUnit.DAYS);
        UUID id = createObligation(session, "Pagar seguro", dueSoon);

        mockMvc.perform(get("/api/v1/obligations").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueSoon[0].title").value("Pagar seguro"))
                .andExpect(jsonPath("$.notifications[0].type").value("OBLIGATION_DUE"));

        mockMvc.perform(patch("/api/v1/obligations/" + id + "/complete").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void searchReturnsRecordsDocsAndObligations() throws Exception {
        MockHttpSession session = register("search@example.com", "Search User");
        mockMvc.perform(post("/api/v1/records").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "ASSET", "title", "Contrato Casa", "status", "ACTIVE"))))
                .andExpect(status().isCreated());
        createObligation(session, "Renovar Contrato", Instant.now().plus(3, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/v1/search").session(session).param("q", "Contrato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].title").value("Contrato Casa"))
                .andExpect(jsonPath("$.obligations[0].title").value("Renovar Contrato"));
    }

    @Test
    void cannotAccessAnotherHouseholdObligation() throws Exception {
        MockHttpSession a = register("oa@example.com", "A");
        MockHttpSession b = register("ob@example.com", "B");
        UUID id = createObligation(a, "Privada", Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/v1/obligations/" + id).session(b)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/obligations").session(b)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
