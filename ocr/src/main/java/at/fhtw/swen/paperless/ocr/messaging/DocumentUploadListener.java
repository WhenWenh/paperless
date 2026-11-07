package at.fhtw.swen.paperless.ocr.messaging;

import at.fhtw.swen.paperless.share.messaging.DocumentUploadedMessage;
import at.fhtw.swen.paperless.share.QueueNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import at.fhtw.swen.paperless.ocr.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentUploadListener {

    private final DocumentStorageService storageService;

    @RabbitListener(
            queues = QueueNames.DOCUMENT_UPLOAD_QUEUE
    )
    public void receive(DocumentUploadedMessage message) {
        try {
            log.info(
                    "Received uploaded document {} ('{}') of type {}, stored as {}",
                    message.title(),
                    message.filename(),
                    message.contentType(),
                    message.storageUuid()
            );

            log.info(
                    "Start processing OCR job for: title: {}, documentId={}, storageUuid={}, contentType={}",
                    message.title(),
                    message.documentId(),
                    message.storageUuid(),
                    message.contentType()
            );

            process(message);

            log.info(
                    "Successfully processed document: {}",
                    message.documentId()
            );

        } catch (Exception e) {
            log.error(
                    "OCR processing failed for document {}",
                    message.documentId(),
                    e
            );

            throw new OcrProcessingException(
                    "Failed processing document " + message.documentId(),
                    e
            );
        }
    }

    private void process(DocumentUploadedMessage message) throws IOException {
        Resource resource = storageService.load(message.storageUuid());

        try (InputStream input = resource.getInputStream()) {
            long bytesRead = input.transferTo(OutputStream.nullOutputStream());

            log.info(
                    "Loaded document from storage: documentId={}, storageUuid={}, bytes={}",
                    message.documentId(),
                    message.storageUuid(),
                    bytesRead
            );
        }
    }
}