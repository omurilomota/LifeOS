package com.lifeos.household;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class HouseholdIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private MockHttpSession register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123", "displayName", name))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void ownerCanInviteAndMemberCanAccept() throws Exception {
        MockHttpSession owner = register("owner@example.com", "Owner");
        MockHttpSession member = register("member@example.com", "Member");

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/households/current/invitations").session(owner).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "member@example.com", "role", "MEMBER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitation.invitedEmail").value("member@example.com"))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        JsonNode inviteBody = objectMapper.readTree(inviteResult.getResponse().getContentAsString());
        String token = inviteBody.get("token").asText();

        mockMvc.perform(get("/api/v1/households/current/detail").session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.invitations.length()").value(1));

        mockMvc.perform(post("/api/v1/households/invitations/accept").session(member).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(get("/api/v1/households/current/detail").session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void inviteEmailMismatchIsForbidden() throws Exception {
        MockHttpSession owner = register("owner2@example.com", "Owner2");
        MockHttpSession other = register("other@example.com", "Other");

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/households/current/invitations").session(owner).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "target@example.com", "role", "MEMBER"))))
                .andExpect(status().isCreated()).andReturn();
        String token = objectMapper.readTree(inviteResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(post("/api/v1/households/invitations/accept").session(other).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVITE_EMAIL_MISMATCH"));
    }
}
