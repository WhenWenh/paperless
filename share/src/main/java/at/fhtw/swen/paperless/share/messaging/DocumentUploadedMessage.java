package at.fhtw.swen.paperless.share.messaging;

import java.util.UUID;

public record DocumentUploadedMessage(
        UUID documentId,
        UUID storageUuid,
        UUID ownerId,
        String title,
        String filename,
        String contentType,
        UUID tagId
)
{}
