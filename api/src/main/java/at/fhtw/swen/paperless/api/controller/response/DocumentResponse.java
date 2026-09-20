package at.fhtw.swen.paperless.api.controller.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt,
        Instant updatedAt
) {
}