package at.fhtw.swen.paperless.genai.messaging;

import at.fhtw.swen.paperless.genai.service.DocumentIndexService;
import at.fhtw.swen.paperless.genai.service.SummaryGenerator;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import at.fhtw.swen.paperless.share.messaging.SummaryGeneratedMessage;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OcrCompletedListenerTest {

    private final DocumentIndexService documentIndexService =
            mock(DocumentIndexService.class);

    private final SummaryGenerator summaryGenerator =
            mock(SummaryGenerator.class);

    private final SummaryGeneratedPublisher publisher =
            mock(SummaryGeneratedPublisher.class);

    private final OcrCompletedListener listener =
            new OcrCompletedListener(
                    documentIndexService,
                    summaryGenerator,
                    publisher
            );

    @Test
    void indexesGeneratesAndPublishesWithOriginalDocumentId() {
        OcrCompletedMessage message = message("OCR text");

        when(summaryGenerator.generate(message))
                .thenReturn("Generated summary");

        listener.receive(message);

        InOrder order = inOrder(
                documentIndexService,
                summaryGenerator,
                publisher
        );

        order.verify(documentIndexService).index(message);

        order.verify(summaryGenerator).generate(message);

        order.verify(publisher).publish(
                new SummaryGeneratedMessage(
                        message.documentId(),
                        "Generated summary"
                )
        );
    }

    @Test
    void emptyTextIsIndexedButDoesNotTriggerSummary() {
        OcrCompletedMessage message = message("");

        listener.receive(message);

        verify(documentIndexService).index(message);
        verifyNoInteractions(summaryGenerator, publisher);
    }

    @Test
    void whitespaceTextIsIndexedButDoesNotTriggerSummary() {
        OcrCompletedMessage message = message(" \n\t ");

        listener.receive(message);

        verify(documentIndexService).index(message);
        verifyNoInteractions(summaryGenerator, publisher);
    }

    @Test
    void nullTextIsIndexedButDoesNotTriggerSummary() {
        OcrCompletedMessage message = message(null);

        listener.receive(message);

        verify(documentIndexService).index(message);
        verifyNoInteractions(summaryGenerator, publisher);
    }

    @Test
    void generationFailureOccursAfterIndexingAndIsPropagated() {
        OcrCompletedMessage message = message("OCR text");

        RuntimeException failure =
                new IllegalStateException("Ollama unavailable");

        when(summaryGenerator.generate(message))
                .thenThrow(failure);

        assertThatThrownBy(() -> listener.receive(message))
                .isSameAs(failure);

        verify(documentIndexService).index(message);
        verifyNoInteractions(publisher);
    }

    @Test
    void blankSummaryIsNotPublished() {
        OcrCompletedMessage message = message("OCR text");

        when(summaryGenerator.generate(message))
                .thenReturn("   ");

        assertThatThrownBy(() -> listener.receive(message))
                .isInstanceOf(IllegalStateException.class);

        verify(documentIndexService).index(message);
        verifyNoInteractions(publisher);
    }

    private OcrCompletedMessage message(String text) {
        return new OcrCompletedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test document",
                "test.pdf",
                null,
                "application/pdf",
                text
        );
    }
}