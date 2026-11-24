package at.fhtw.swen.paperless.genai.messaging;

import at.fhtw.swen.paperless.share.QueueNames;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /**
     * Rejected messages are routed to the dead letter queue instead of
     * being requeued endlessly. The empty exchange is the default
     * exchange, which routes by queue name.
     */
    @Bean
    public Queue ocrCompletedQueue() {
        return QueueBuilder
                .durable(QueueNames.OCR_COMPLETED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(
                        QueueNames.OCR_COMPLETED_DEAD_LETTER_QUEUE
                )
                .build();
    }

    @Bean
    public Queue ocrCompletedDeadLetterQueue() {
        return QueueBuilder
                .durable(QueueNames.OCR_COMPLETED_DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue summaryGeneratedQueue() {
        return new Queue(
                QueueNames.SUMMARY_GENERATED_QUEUE, true);
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
