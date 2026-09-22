package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.service.TagService;
import at.fhtw.swen.paperless.api.service.dto.TagDto;
import at.fhtw.swen.paperless.api.service.exception.TagAlreadyExistsException;
import at.fhtw.swen.paperless.api.service.exception.TagInUseException;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Test
    void createTag_shouldReturn201() throws Exception {
        UUID id = UUID.randomUUID();
        when(tagService.createTag(anyString()))
                .thenReturn(TagDto.builder().id(id).name("Finance").build());

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Finance" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Finance"));
    }

    @Test
    void createTag_blankName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.name").exists());

        verify(tagService, never()).createTag(anyString());
    }

    @Test
    void createTag_missingName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void createTag_nameTooLong_shouldReturn400() throws Exception {
        String longName = "a".repeat(51);

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"" + longName + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void createTag_duplicateName_shouldReturn409() throws Exception {
        when(tagService.createTag("finance"))
                .thenThrow(new TagAlreadyExistsException("finance"));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "finance" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tag already exists: finance"));
    }

    @Test
    void getAllTags_shouldReturnList() throws Exception {
        UUID id = UUID.randomUUID();
        when(tagService.getAllTags())
                .thenReturn(List.of(TagDto.builder().id(id).name("Finance").build()));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Finance"));
    }

    @Test
    void getAllTags_noTags_shouldReturnEmptyList() throws Exception {
        when(tagService.getAllTags()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteTag_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/tags/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTag_unknownTag_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TagNotFoundException(id)).when(tagService).deleteTag(id);

        mockMvc.perform(delete("/api/v1/tags/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tag not found: " + id));
    }

    @Test
    void deleteTag_tagInUse_shouldReturn409WithDocumentCount() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TagInUseException(id, 3)).when(tagService).deleteTag(id);

        mockMvc.perform(delete("/api/v1/tags/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Tag " + id + " is still assigned to 3 document(s)"));
    }

    @Test
    void deleteTag_invalidId_shouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/v1/tags/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid parameter format"));
    }
}
