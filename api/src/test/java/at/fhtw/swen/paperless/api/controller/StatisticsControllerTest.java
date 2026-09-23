package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;
import at.fhtw.swen.paperless.api.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    @Test
    void getTagStatistics_shouldReturnStatistics() throws Exception {
        UUID tagId = UUID.randomUUID();

        when(statisticsService.getTagStatistics())
                .thenReturn(List.of(new TagStatisticsResponse(tagId, "Finance", 5)));

        mockMvc.perform(get("/api/v1/statistics/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tagId").value(tagId.toString()))
                .andExpect(jsonPath("$[0].tagName").value("Finance"))
                .andExpect(jsonPath("$[0].documentCount").value(5));
    }
}
