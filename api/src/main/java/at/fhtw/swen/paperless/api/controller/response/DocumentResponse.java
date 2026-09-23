package at.fhtw.swen.paperless.api.controller.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DocumentResponse(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt,
        Instant updatedAt,
        UUID tagId,
        String tagName
) {
}