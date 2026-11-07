package at.fhtw.swen.paperless.ocr.service.impl;

import at.fhtw.swen.paperless.ocr.ocr.TesseractExecutor;
import at.fhtw.swen.paperless.ocr.service.OcrService;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TesseractOcrService implements OcrService {

    private final TesseractExecutor executor;

    @Override
    public OcrCompletedMessage process(
            UUID documentId,
            UUID storageUuid,
            UUID ownerId,
            String title,
            String filename,
            UUID tagId,
            String contentType,
            Resource document
    ) {
        String text = executor.extractText(
                document,
                contentType
        );
        
        return new OcrCompletedMessage(
                documentId,
                storageUuid,
                ownerId,
                title,
                filename,
                tagId,
                contentType,
                text
        );
    }
}