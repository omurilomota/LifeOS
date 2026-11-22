package com.lifeos.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private MockHttpSession register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123", "displayName", name))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private UUID createRecord(MockHttpSession session, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/records").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "ASSET", "title", title, "status", "ACTIVE"))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID uploadTxt(MockHttpSession session, String filename, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/plain", content.getBytes());
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents").file(file).session(session).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value(filename))
                .andExpect(jsonPath("$.extractionStatus").value("READY"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    @Test
    void uploadListDownloadLinkAndDeleteFlow() throws Exception {
        MockHttpSession session = register("docs@example.com", "Docs User");
        UUID recordId = createRecord(session, "Notebook");
        UUID docId = uploadTxt(session, "nota.txt", "conteudo importante");

        mockMvc.perform(get("/api/v1/documents").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(docId.toString()));

        mockMvc.perform(get("/api/v1/documents/" + docId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("text/plain"));

        MvcResult download = mockMvc.perform(get("/api/v1/documents/" + docId + "/download").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(download.getResponse().getContentAsString()).contains("conteudo importante");

        mockMvc.perform(post("/api/v1/documents/" + docId + "/links").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("recordId", recordId.toString(), "linkType", "attachment"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkType").value("ATTACHMENT"));

        mockMvc.perform(get("/api/v1/documents/" + docId + "/links").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordId").value(recordId.toString()));

        mockMvc.perform(delete("/api/v1/documents/" + docId).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents/" + docId).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotAccessAnotherHouseholdDocument() throws Exception {
        MockHttpSession a = register("doc-a@example.com", "A");
        MockHttpSession b = register("doc-b@example.com", "B");
        UUID docId = uploadTxt(a, "private.txt", "secret");

        mockMvc.perform(get("/api/v1/documents/" + docId).session(b)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download").session(b)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/documents/" + docId).session(b).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        MockHttpSession session = register("badfile@example.com", "Bad");
        MockMultipartFile file = new MockMultipartFile("file", "evil.exe", "application/x-msdownload", "MZ".getBytes());
        mockMvc.perform(multipart("/api/v1/documents").file(file).session(session).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }
}
