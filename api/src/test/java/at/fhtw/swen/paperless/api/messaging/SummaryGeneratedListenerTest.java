package at.fhtw.swen.paperless.api.messaging;

import at.fhtw.swen.paperless.api.service.DocumentService;
import at.fhtw.swen.paperless.share.messaging.SummaryGeneratedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SummaryGeneratedListenerTest {

    private final DocumentService documentService = mock(DocumentService.class);
    private final SummaryGeneratedListener listener =
            new SummaryGeneratedListener(documentService);

    @Test
    void handle_shouldStoreSummaryWithOriginalDocumentId() {
        UUID documentId = UUID.randomUUID();
        String summary = "Die Rechnung beträgt 120 Euro.";
        when(documentService.updateSummary(documentId, summary)).thenReturn(true);

        listener.handle(new SummaryGeneratedMessage(documentId, summary));

        verify(documentService).updateSummary(documentId, summary);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n "})
    void handle_shouldIgnoreMissingOrBlankSummary(String summary) {
        listener.handle(new SummaryGeneratedMessage(UUID.randomUUID(), summary));

        verifyNoInteractions(documentService);
    }

    @Test
    void handle_shouldIgnoreMissingDocumentId() {
        listener.handle(new SummaryGeneratedMessage(null, "Summary"));

        verifyNoInteractions(documentService);
    }
}
