package at.fhtw.swen.paperless.genai.messaging;

import at.fhtw.swen.paperless.genai.service.DocumentIndexService;
import at.fhtw.swen.paperless.share.QueueNames;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrCompletedListener {

    private final DocumentIndexService documentIndexService;

    @RabbitListener(queues = QueueNames.OCR_COMPLETED_QUEUE)
    public void receive(OcrCompletedMessage message){
        try {
            log.info(
                    "Indexing OCR result: documentId={}, storageUuid={}",
                    message.documentId(),
                    message.storageUuid()
            );

            documentIndexService.index(message);

            log.info(
                    "Successfully indexed document: documentId={}",
                    message.documentId()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Indexing failed: documentId={}, storageUuid={}",
                    message.documentId(),
                    message.storageUuid(),
                    exception
            );

            throw exception;
        }
    }
}
