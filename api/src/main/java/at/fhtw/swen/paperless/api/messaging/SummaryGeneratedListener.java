package at.fhtw.swen.paperless.api.messaging;

import at.fhtw.swen.paperless.api.service.DocumentService;
import at.fhtw.swen.paperless.share.QueueNames;
import at.fhtw.swen.paperless.share.messaging.SummaryGeneratedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryGeneratedListener {

    private final DocumentService documentService;

    @RabbitListener(queues = QueueNames.SUMMARY_GENERATED_QUEUE)
    public void handle(SummaryGeneratedMessage message) {
        if (message.documentId() == null
                || message.summary() == null
                || message.summary().isBlank()) {
            log.warn(
                    "Ignoring invalid generated summary: documentId={}",
                    message.documentId()
            );
            return;
        }

        boolean updated = documentService.updateSummary(
                message.documentId(),
                message.summary()
        );

        if (updated) {
            log.info(
                    "Stored generated summary: documentId={}",
                    message.documentId()
            );
        } else {
            log.info(
                    "Document missing, skipping generated summary: documentId={}",
                    message.documentId()
            );
        }
    }
}