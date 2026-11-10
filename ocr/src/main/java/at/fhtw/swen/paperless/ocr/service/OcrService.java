package at.fhtw.swen.paperless.ocr.service;

import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface OcrService {

    OcrCompletedMessage process(
            UUID documentId,
            UUID storageUuid,
            UUID ownerId,
            String title,
            String filename,
            UUID tagId,
            String contentType,
            Resource document
    );
}