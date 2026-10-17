package at.fhtw.swen.paperless.ocr.messaging;

import at.fhtw.swen.paperless.share.DocumentUploadedMessage;
import at.fhtw.swen.paperless.share.QueueNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DocumentUploadListener {

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

    private void process(DocumentUploadedMessage message) {
        // TODO: OCR later
    }
}