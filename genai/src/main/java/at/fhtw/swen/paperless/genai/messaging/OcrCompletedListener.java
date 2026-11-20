package at.fhtw.swen.paperless.genai.messaging;

import at.fhtw.swen.paperless.genai.service.DocumentIndexService;
import at.fhtw.swen.paperless.genai.service.SummaryGenerator;
import at.fhtw.swen.paperless.share.QueueNames;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import at.fhtw.swen.paperless.share.messaging.SummaryGeneratedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrCompletedListener {

    private final DocumentIndexService documentIndexService;
    private final SummaryGenerator summaryGenerator;
    private final SummaryGeneratedPublisher summaryGeneratedPublisher;

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

        if (message.text() == null || message.text().isBlank()) {
            log.info(
                    "Skipping summary: documentId={}, OCR text is empty",
                    message.documentId()
            );

            return;
        }

        String summary;

        try {
            log.info(
                    "Generating summary: documentId={}",
                    message.documentId()
            );

            summary = summaryGenerator.generate(message);

            if (summary == null || summary.isBlank()) {
                throw new IllegalStateException(
                        "Generated summary is empty for document "
                                + message.documentId()
                );
            }

            log.info(
                    "Successfully generated summary: documentId={}",
                    message.documentId()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Summary generation failed: documentId={}, errorType={}",
                    message.documentId(),
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }

        SummaryGeneratedMessage summaryMessage =
                new SummaryGeneratedMessage(
                        message.documentId(),
                        summary
                );

        try {
            summaryGeneratedPublisher.publish(summaryMessage);

            log.info(
                    "Published generated summary: documentId={}",
                    message.documentId()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Summary publishing failed: documentId={}, errorType={}",
                    message.documentId(),
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }
}
