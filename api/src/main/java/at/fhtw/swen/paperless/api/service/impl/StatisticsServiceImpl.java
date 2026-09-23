package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final DocumentRepository documentRepository;

    @Override
    public List<TagStatisticsResponse> getTagStatistics() {
        List<TagStatisticsResponse> statistics =
                new ArrayList<>(documentRepository.getTagStatistics());
        long withoutTag = documentRepository.countDocumentsWithoutTag();

        if (withoutTag > 0) {
            statistics.add(
                    new TagStatisticsResponse(
                            null,
                            "Untagged",
                            withoutTag
                    )
            );
        }
        return statistics;
    }
}
