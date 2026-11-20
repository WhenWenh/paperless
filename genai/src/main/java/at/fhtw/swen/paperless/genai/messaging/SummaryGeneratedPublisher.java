package at.fhtw.swen.paperless.genai.messaging;

import at.fhtw.swen.paperless.share.QueueNames;
import at.fhtw.swen.paperless.share.messaging.SummaryGeneratedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummaryGeneratedPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(SummaryGeneratedMessage message) {
        rabbitTemplate.convertAndSend(
                QueueNames.SUMMARY_GENERATED_QUEUE,
                message
        );
    }
}