package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
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

        Tag tag = new Tag();
        tag.setId(id);
        tag.setName("Finance");

        when(tagService.createTag(anyString())).thenReturn(tag);

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {
              "name":"Finance"
            }
            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Finance"));
    }

    @Test
    void getAllTags_shouldReturnList() throws Exception {
        UUID id = UUID.randomUUID();

        Tag tag = new Tag();
        tag.setId(id);
        tag.setName("Finance");

        when(tagService.getAllTags()).thenReturn(List.of(tag));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Finance"));
    }

    @Test
    void deleteTag_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tags/" + id))
                .andExpect(status().isNoContent());
    }
}
