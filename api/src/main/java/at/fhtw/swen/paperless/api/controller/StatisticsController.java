package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;
import at.fhtw.swen.paperless.api.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/tags")
    public ResponseEntity<List<TagStatisticsResponse>> getTagStatistics() {
        return ResponseEntity.ok(
                statisticsService.getTagStatistics()
        );
    }
}
