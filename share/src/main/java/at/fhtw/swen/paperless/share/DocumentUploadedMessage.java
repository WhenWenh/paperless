package at.fhtw.swen.paperless.share;

import java.util.UUID;

public record DocumentUploadedMessage(
        String title,
        String filename,
        String contentType,
        UUID documentId,
        UUID storageUuid
)
{}
