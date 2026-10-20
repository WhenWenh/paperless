package at.fhtw.swen.paperless.api.messaging;

import at.fhtw.swen.paperless.api.service.exception.OcrJobPublishException;
import at.fhtw.swen.paperless.share.DocumentUploadedMessage;
import at.fhtw.swen.paperless.share.QueueNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentUploaded(
            DocumentUploadedMessage message
    ) {
        try {
            log.info(
                    "Publishing OCR job: documentId={}, storageUuid={}",
                    message.documentId(),
                    message.storageUuid()
            );

            rabbitTemplate.convertAndSend(
                    QueueNames.DOCUMENT_UPLOAD_QUEUE,
                    message
            );

        } catch (AmqpException exception) {
            log.error(
                    "OCR job publishing failed: documentId={}, storageUuid={}",
                    message.documentId(),
                    message.storageUuid(),
                    exception
            );

            throw new OcrJobPublishException(
                    message.documentId(),
                    exception
            );
        }
    }
}