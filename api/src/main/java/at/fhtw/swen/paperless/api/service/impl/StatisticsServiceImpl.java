package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.security.CurrentUser;
import at.fhtw.swen.paperless.api.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final DocumentRepository documentRepository;
    private final CurrentUser currentUser;


    @Override
    public List<TagStatisticsResponse> getTagStatistics() {
        UUID ownerId = currentUser.id();
        List<TagStatisticsResponse> statistics =
                new ArrayList<>(documentRepository.getTagStatistics(ownerId));
        long withoutTag = documentRepository.countDocumentsWithoutTag(ownerId);

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
