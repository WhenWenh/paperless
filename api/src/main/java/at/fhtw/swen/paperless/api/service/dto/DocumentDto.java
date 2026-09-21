package at.fhtw.swen.paperless.api.service.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(
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