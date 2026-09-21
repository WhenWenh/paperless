package at.fhtw.swen.paperless.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import at.fhtw.swen.paperless.api.service.DocumentService;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void createDocument_shouldReturn201() throws Exception {
        UUID id = UUID.randomUUID();

        when(documentService.createDocument(any())).thenReturn(
                new DocumentDto(
                        id,
                        "Title",
                        "file.pdf",
                        "application/pdf",
                        123L,
                        null,
                        null, null, null
                ));

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "title":"Title",
                          "originalFilename":"file.pdf",
                          "contentType":"application/pdf",
                          "fileSize":10
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void createDocument_shouldReturn400WhenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"originalFilename\":\"\",\"contentType\":\"\",\"fileSize\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDocument_shouldReturn404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.getDocument(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/documents/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllDocuments_shouldReturnList() throws Exception {
        when(documentService.getAllDocuments()).thenReturn(List.of(
                new DocumentDto(
                        UUID.randomUUID(),
                        "Title",
                        "file.pdf",
                        "application/pdf",
                        123L,
                        null,
                        null, null, null
                )));
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getDocument_shouldReturnDocument() throws Exception {
        UUID id = UUID.randomUUID();

        when(documentService.getDocument(id)).thenReturn(Optional.of(
                new DocumentDto(
                        id,
                        "Title",
                        "file.pdf",
                        "application/pdf",
                        123L,
                        null,
                        null, null, null
                )));

        mockMvc.perform(get("/api/v1/documents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void deleteDocument_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/documents/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void getDocumentsByTag_shouldReturnDocuments() throws Exception {
        UUID tagId = UUID.randomUUID();
        when(documentService.getDocumentsByTag("Finance")).thenReturn(List.of(
                new DocumentDto(UUID.randomUUID(), "Title", "file.pdf", "application/pdf", 123L, null, null, tagId, "Finance")));

        mockMvc.perform(get("/api/v1/documents").param("tag", "Finance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tagName").value("Finance"));
    }

    @Test
    void updateDocumentTag_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        when(documentService.updateDocumentTag(eq(id), eq(tagId))).thenReturn(Optional.of(
                new DocumentDto(id, "Title", "file.pdf", "application/pdf", 123L, null, null, tagId, "Finance")));

        mockMvc.perform(put("/api/v1/documents/" + id + "/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagId\":\"" + tagId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(tagId.toString()))
                .andExpect(jsonPath("$.tagName").value("Finance"));
    }

    @Test
    void updateDocumentTag_shouldReturn404WhenDocumentMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.updateDocumentTag(eq(id), eq(null))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/documents/" + id + "/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagId\":null}"))
                .andExpect(status().isNotFound());
    }
}
