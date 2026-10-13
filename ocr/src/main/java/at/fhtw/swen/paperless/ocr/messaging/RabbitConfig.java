package at.fhtw.swen.paperless.ocr.messaging;

import at.fhtw.swen.paperless.share.QueueNames;
import org.springframework.amqp.core.Queue;
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

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
