package at.fhtw.swen.paperless.genai.service;

import at.fhtw.swen.paperless.genai.index.DocumentIndex;
import at.fhtw.swen.paperless.genai.index.DocumentIndexRepository;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentIndexServiceTest {

    private final DocumentIndexRepository repository =
            mock(DocumentIndexRepository.class);

    private final DocumentIndexService service =
            new DocumentIndexService(repository);

    @Test
    void index_shouldReuseDocumentIdForUpdatedText() {
        UUID documentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        service.index(message(documentId, ownerId, "first text"));
        service.index(message(documentId, ownerId, "updated text"));

        ArgumentCaptor<DocumentIndex> captor =
                ArgumentCaptor.forClass(DocumentIndex.class);

        verify(repository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(DocumentIndex::getDocumentId)
                .containsExactly(documentId, documentId);

        assertThat(captor.getAllValues())
                .extracting(DocumentIndex::getContent)
                .containsExactly("first text", "updated text");

        assertThat(captor.getValue().getOwnerId())
                .isEqualTo(ownerId);

        assertThat(captor.getValue().getOriginalFilename())
                .isEqualTo("invoice.pdf");
    }

    @Test
    void index_shouldAllowEmptyText() {
        UUID documentId = UUID.randomUUID();

        service.index(message(
                documentId,
                UUID.randomUUID(),
                ""
        ));

        verify(repository).save(argThat(index ->
                index.getDocumentId().equals(documentId)
                        && index.getContent().isEmpty()
        ));
    }

    @Test
    void index_shouldPropagateRepositoryFailure() {
        RuntimeException failure =
                new IllegalStateException("Elasticsearch unavailable");

        when(repository.save(any(DocumentIndex.class)))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.index(message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "text"
        ))).isSameAs(failure);
    }

    private OcrCompletedMessage message(
            UUID documentId,
            UUID ownerId,
            String text
    ) {
        return new OcrCompletedMessage(
                documentId,
                UUID.randomUUID(),
                ownerId,
                "Invoice",
                "invoice.pdf",
                null,
                "application/pdf",
                text
        );
    }
}