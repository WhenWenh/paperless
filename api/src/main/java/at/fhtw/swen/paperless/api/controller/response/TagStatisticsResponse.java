package at.fhtw.swen.paperless.api.controller.response;

import java.util.UUID;

public record TagStatisticsResponse(
        UUID tagId,
        String tagName,
        long documentCount
) {
}
