package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    void getTagStatistics_shouldReturnStatisticsPerTag() {
        UUID tagId = UUID.randomUUID();
        when(documentRepository.getTagStatistics())
                .thenReturn(List.of(new TagStatisticsResponse(tagId, "Finance", 10)));
        when(documentRepository.countDocumentsWithoutTag()).thenReturn(0L);

        List<TagStatisticsResponse> result = statisticsService.getTagStatistics();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tagName()).isEqualTo("Finance");
        assertThat(result.get(0).documentCount()).isEqualTo(10);
    }

    @Test
    void getTagStatistics_shouldIncludeUntaggedDocuments() {
        when(documentRepository.getTagStatistics()).thenReturn(List.of());
        when(documentRepository.countDocumentsWithoutTag()).thenReturn(3L);

        List<TagStatisticsResponse> result = statisticsService.getTagStatistics();

        assertThat(result).hasSize(1);
        TagStatisticsResponse untagged = result.get(0);
        assertThat(untagged.tagId()).isNull();
        assertThat(untagged.tagName()).isEqualTo("Untagged");
        assertThat(untagged.documentCount()).isEqualTo(3);
    }

    @Test
    void getTagStatistics_shouldNotAddUntaggedWhenNoneExist() {
        when(documentRepository.getTagStatistics()).thenReturn(List.of());
        when(documentRepository.countDocumentsWithoutTag()).thenReturn(0L);

        assertThat(statisticsService.getTagStatistics()).isEmpty();
    }
}
