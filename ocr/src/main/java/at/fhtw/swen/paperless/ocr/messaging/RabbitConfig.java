package at.fhtw.swen.paperless.ocr.messaging;

import at.fhtw.swen.paperless.share.QueueNames;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue documentUploadQueue() {
        return new Queue(
                QueueNames.DOCUMENT_UPLOAD_QUEUE,
                true
        );
    }

    /**
     * Declared with the same arguments as in the genai module.
     * A queue that already exists cannot be redeclared with
     * different arguments, so both declarations must match.
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
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
