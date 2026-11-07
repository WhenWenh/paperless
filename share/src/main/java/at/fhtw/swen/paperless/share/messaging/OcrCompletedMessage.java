package at.fhtw.swen.paperless.share.messaging;

import java.util.UUID;

public record OcrCompletedMessage(
        UUID documentId,
        UUID storageUuid,
        UUID ownerId,
        String title,
        String originalFilename,
        UUID tagId,
        String contentType,
        String text
) {
}
