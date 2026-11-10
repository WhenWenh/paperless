package at.fhtw.swen.paperless.ocr.service.impl;

import at.fhtw.swen.paperless.ocr.service.OcrCompletedPublisher;
import at.fhtw.swen.paperless.share.QueueNames;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitOcrCompletedPublisher implements OcrCompletedPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(OcrCompletedMessage result) {
        rabbitTemplate.convertAndSend(
                QueueNames.OCR_COMPLETED_QUEUE,
                result
        );

        log.info(
                "OCR result sent: documentId={}, storageUuid={}, characters={}",
                result.documentId(),
                result.storageUuid(),
                result.text().length()
        );
    }
}