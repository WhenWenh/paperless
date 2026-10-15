package at.fhtw.swen.paperless.api.messaging;

import at.fhtw.swen.paperless.api.service.exception.OcrJobPublishException;
import at.fhtw.swen.paperless.share.DocumentUploadedMessage;
import at.fhtw.swen.paperless.share.QueueNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DocumentEventPublisher publisher;

    @Test
    void publishDocumentUploaded_shouldSendMessage() {
        DocumentUploadedMessage message = message();

        publisher.publishDocumentUploaded(message);

        verify(rabbitTemplate).convertAndSend(
                QueueNames.DOCUMENT_UPLOAD_QUEUE,
                message
        );
    }

    @Test
    void publishDocumentUploaded_shouldWrapAmqpException() {
        DocumentUploadedMessage message = message();

        AmqpException cause =
                new AmqpException("RabbitMQ unavailable");

        doThrow(cause)
                .when(rabbitTemplate)
                .convertAndSend(
                        QueueNames.DOCUMENT_UPLOAD_QUEUE,
                        message
                );

        assertThatThrownBy(
                () -> publisher.publishDocumentUploaded(message)
        )
                .isInstanceOf(OcrJobPublishException.class)
                .hasCause(cause)
                .hasMessageContaining(message.documentId().toString());
    }

    private DocumentUploadedMessage message() {
        return new DocumentUploadedMessage(
                "Invoice",
                "invoice.pdf",
                "application/pdf",
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}