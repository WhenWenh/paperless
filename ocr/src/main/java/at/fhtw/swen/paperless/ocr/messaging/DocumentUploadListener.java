package at.fhtw.swen.paperless.ocr.messaging;

import at.fhtw.swen.paperless.ocr.service.DocumentStorageService;
import at.fhtw.swen.paperless.ocr.service.OcrCompletedPublisher;
import at.fhtw.swen.paperless.ocr.service.OcrService;
import at.fhtw.swen.paperless.share.QueueNames;
import at.fhtw.swen.paperless.share.messaging.DocumentUploadedMessage;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentUploadListener {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/tiff",
            "image/bmp",
            "image/gif",
            "image/webp"
    );

    private final DocumentStorageService storageService;
    private final OcrService ocrService;
    private final OcrCompletedPublisher publisher;

    @RabbitListener(queues = QueueNames.DOCUMENT_UPLOAD_QUEUE)
    public void receive(DocumentUploadedMessage message) {
        try {
            log.info(
                    "Received OCR job: documentId={}, storageUuid={}, contentType={}",
                    message.documentId(),
                    message.storageUuid(),
                    message.contentType()
            );

            if (!isSupported(message.contentType())) {
                log.warn(
                        "Skipping unsupported document: documentId={}, contentType={}",
                        message.documentId(),
                        message.contentType()
                );
                return;
            }

            process(message);

            log.info(
                    "Successfully processed document: {}",
                    message.documentId()
            );

        } catch (Exception exception) {
            log.error(
                    "OCR processing failed: documentId={}, storageUuid={}",
                    message.documentId(),
                    message.storageUuid(),
                    exception
            );

            throw new OcrProcessingException(
                    "Failed processing document " + message.documentId(),
                    exception
            );
        }
    }

    private boolean isSupported(String contentType) {
        return contentType != null
                && SUPPORTED_CONTENT_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT)
        );
    }

    private void process(DocumentUploadedMessage message) {
        Resource resource = storageService.load(
                message.storageUuid()
        );

        log.info(
                "Loaded document for OCR: documentId={}, storageUuid={}",
                message.documentId(),
                message.storageUuid()
        );

        OcrCompletedMessage result = ocrService.process(
                message.documentId(),
                message.storageUuid(),
                message.ownerId(),
                message.title(),
                message.filename(),
                message.tagId(),
                message.contentType(),
                resource
        );

        publisher.publish(result);
    }
}