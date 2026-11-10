package at.fhtw.swen.paperless.ocr.messaging;

import at.fhtw.swen.paperless.ocr.ocr.TesseractExecutor;
import at.fhtw.swen.paperless.ocr.service.DocumentStorageService;
import at.fhtw.swen.paperless.ocr.service.OcrCompletedPublisher;
import at.fhtw.swen.paperless.ocr.service.impl.TesseractOcrService;
import at.fhtw.swen.paperless.share.messaging.DocumentUploadedMessage;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DocumentUploadListenerTests {

    private DocumentStorageService storage;
    private TesseractExecutor executor;
    private OcrCompletedPublisher publisher;
    private DocumentUploadListener listener;
    private DocumentUploadedMessage message;
    private Resource document;

    @BeforeEach
    void setUp() {
        storage = mock(DocumentStorageService.class);
        executor = mock(TesseractExecutor.class);
        publisher = mock(OcrCompletedPublisher.class);

        // Exercise the real service's handling of empty OCR results.
        listener = new DocumentUploadListener(
                storage, new TesseractOcrService(executor), publisher
        );
        message = new DocumentUploadedMessage(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Invoice", "invoice.pdf", "application/pdf", UUID.randomUUID()
        );
        // No real PDF parser is used: the external executor is mocked.
        document = new ByteArrayResource(new byte[]{1, 2, 3});
    }

    @ParameterizedTest
    @ValueSource(strings = {"Invoice number 12345", "", " \n\t"})
    void receive_shouldPublishTextIncludingEmptyResultsWithMetadata(String text) {
        when(storage.load(message.storageUuid())).thenReturn(document);
        when(executor.extractText(document, message.contentType())).thenReturn(text);

        listener.receive(message);

        verify(publisher).publish(new OcrCompletedMessage(
                message.documentId(), message.storageUuid(), message.ownerId(),
                message.title(), message.filename(), message.tagId(),
                message.contentType(), text
        ));
        verifyNoMoreInteractions(publisher);
    }

    @Test
    void receive_shouldNotPublishWhenExtractionFails() {
        var failure = new IllegalStateException("PDF conversion failed");
        when(storage.load(message.storageUuid())).thenReturn(document);
        when(executor.extractText(document, message.contentType())).thenThrow(failure);

        assertThatThrownBy(() -> listener.receive(message))
                .isInstanceOf(OcrProcessingException.class)
                .hasMessageContaining(message.documentId().toString())
                .hasCause(failure);

        verifyNoInteractions(publisher);
    }

    @Test
    void receive_shouldNotExtractOrPublishWhenDownloadFails() {
        var failure = new IllegalStateException("Object not found");
        when(storage.load(message.storageUuid())).thenThrow(failure);

        assertThatThrownBy(() -> listener.receive(message))
                .isInstanceOf(OcrProcessingException.class)
                .hasMessageContaining(message.documentId().toString())
                .hasCause(failure);

        verifyNoInteractions(executor, publisher);
    }

    @Test
    void receive_shouldSkipUnsupportedContentType() {
        var unsupported = new DocumentUploadedMessage(
                message.documentId(), message.storageUuid(), message.ownerId(),
                message.title(), "notes.txt", "text/plain", null
        );

        listener.receive(unsupported);

        verifyNoInteractions(storage, executor, publisher);
    }
}
