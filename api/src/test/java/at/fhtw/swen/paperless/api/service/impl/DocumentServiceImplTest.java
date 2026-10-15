package at.fhtw.swen.paperless.api.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.fhtw.swen.paperless.api.service.exception.OcrJobPublishException;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import at.fhtw.swen.paperless.api.service.mapper.DocumentMapper;
import at.fhtw.swen.paperless.api.security.CurrentUser;
import at.fhtw.swen.paperless.api.service.DocumentStorageService;
import at.fhtw.swen.paperless.api.service.DocumentValidationService;
import at.fhtw.swen.paperless.api.service.dto.CreateDocumentCommand;
import at.fhtw.swen.paperless.api.service.exception.InvalidUploadException;

import at.fhtw.swen.paperless.api.messaging.DocumentEventPublisher;
import at.fhtw.swen.paperless.share.DocumentUploadedMessage;

import org.springframework.mock.web.MockMultipartFile;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {
    private final UUID ownerId = UUID.randomUUID();
    @Mock private CurrentUser currentUser;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private DocumentValidationService documentValidationService;

    @Mock
    private DocumentEventPublisher documentEventPublisher;

    private final MockMultipartFile file =
            new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[]{1, 2, 3});

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    void createDocument_shouldPersistFileMetadataAndReturnDto() {
        when(currentUser.id()).thenReturn(ownerId);
        CreateDocumentCommand command = new CreateDocumentCommand("Title", null);
        UUID storageId = UUID.randomUUID();
        Document saved = Document.builder()
                .id(UUID.randomUUID())
                .title("Title")
                .originalFilename("file.pdf")
                .contentType("application/pdf")
                .storageUuid(storageId)
                .build();

        DocumentDto output = DocumentDto.builder().id(saved.getId()).title("Title").build();
        when(documentStorageService.store(file)).thenReturn(storageId);
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(documentMapper.toDto(saved)).thenReturn(output);

        assertThat(documentService.createDocument(command, file)).isEqualTo(output);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        Document entity = captor.getValue();
        assertThat(entity.getOwnerId()).isEqualTo(ownerId);
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getOriginalFilename()).isEqualTo("file.pdf");
        assertThat(entity.getContentType()).isEqualTo("application/pdf");
        assertThat(entity.getFileSize()).isEqualTo(3L);
        assertThat(entity.getStorageUuid()).isEqualTo(storageId);
        assertThat(entity.getTag()).isNull();
        verify(documentValidationService).validateCreate(command, file);
        verify(documentStorageService, never()).delete(any());
        verify(documentEventPublisher).publishDocumentUploaded(
                new DocumentUploadedMessage(
                        "Title",
                        "file.pdf",
                        "application/pdf",
                        saved.getId(),
                        storageId
                )
        );
    }
    @Test
    void getDocument_shouldReturnEmptyWhenNotFound() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID id = UUID.randomUUID();
        when(documentRepository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.empty());

        assertThat(documentService.getDocument(id)).isEmpty();
    }

    @Test
    void getAllDocuments_shouldReturnMappedList() {
        when(currentUser.id()).thenReturn(ownerId);
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .build();

        DocumentDto dto = DocumentDto.builder()
                .id(doc.getId())
                .title("T")
                .originalFilename("f")
                .contentType("c")
                .fileSize(1L)
                .build();

        List<Document> documents = List.of(doc);

        when(documentRepository.findAllByOwnerId(ownerId)).thenReturn(documents);
        when(documentMapper.toDto(doc)).thenReturn(dto);

        assertThat(documentService.getAllDocuments())
                .containsExactly(dto);

        verify(documentMapper).toDto(doc);
    }

    @Test
    void deleteDocument_shouldInvokeRepository() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID id = UUID.randomUUID();
        Document document = Document.builder().id(id).ownerId(ownerId).storageUuid(UUID.randomUUID()).build();
        when(documentRepository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.of(document));
        documentService.deleteDocument(id);
        verify(documentStorageService).delete(document.getStorageUuid());
        verify(documentRepository).delete(document);
    }

    @Test
    void createDocument_withTag_shouldAttachTag() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID tagId = UUID.randomUUID();
        Tag tag = new Tag();
        tag.setId(tagId);
        tag.setName("Finance");
        CreateDocumentCommand input = new CreateDocumentCommand("Title", tagId);

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentService.createDocument(input, file);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getTag()).isSameAs(tag);
    }

    @Test
    void createDocument_withMissingTag_shouldThrowAndNotSave() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID tagId = UUID.randomUUID();
        CreateDocumentCommand input = new CreateDocumentCommand("Title", tagId);

        UUID storageId = UUID.randomUUID();
        when(documentStorageService.store(file)).thenReturn(storageId);
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocument(input, file))
                .isInstanceOf(TagNotFoundException.class);
        verify(documentRepository, never()).save(any());
        verify(documentStorageService).delete(storageId);
        verifyNoInteractions(documentEventPublisher);
    }

    @Test
    void getDocumentsByTag_shouldReturnDocuments() {
        when(currentUser.id()).thenReturn(ownerId);
        Document doc = new Document();
        DocumentDto dto = new DocumentDto(UUID.randomUUID(), "T", "f", "c", 1L, null, null, null, "Finance");

        when(documentRepository.findByOwnerIdAndTag_Name(ownerId, "Finance")).thenReturn(List.of(doc));
        when(documentMapper.toDto(doc)).thenReturn(dto);

        assertThat(documentService.getDocumentsByTag("Finance")).containsExactly(dto);
    }

    @Test
    void updateDocumentTag_withNull_shouldRemoveTag() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID id = UUID.randomUUID();
        Document doc = new Document();
        doc.setTag(new Tag());

        when(documentRepository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.of(doc));
        when(documentRepository.save(doc)).thenReturn(doc);
        when(documentMapper.toDto(doc)).thenReturn(
                new DocumentDto(id, "T", "f", "c", 1L, null, null, null, null));

        assertThat(documentService.updateDocumentTag(id, null)).isPresent();
        assertThat(doc.getTag()).isNull();
    }

    @Test
    void updateDocumentTag_withTag_shouldAssignTag() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID id = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        Document doc = new Document();
        Tag tag = new Tag();
        tag.setId(tagId);

        when(documentRepository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.of(doc));
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(documentRepository.save(doc)).thenReturn(doc);
        when(documentMapper.toDto(doc)).thenReturn(
                new DocumentDto(id, "T", "f", "c", 1L, null, null, tagId, null));

        assertThat(documentService.updateDocumentTag(id, tagId)).isPresent();
        assertThat(doc.getTag()).isSameAs(tag);
    }

    @Test
    void updateDocumentTag_missingTag_shouldThrow() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID id = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        when(documentRepository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.of(new Document()));
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.updateDocumentTag(id, tagId))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void updateDocumentTag_missingDocument_shouldReturnEmpty() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID id = UUID.randomUUID();
        when(documentRepository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.empty());

        assertThat(documentService.updateDocumentTag(id, UUID.randomUUID())).isEmpty();
    }
    @Test
    void createDocument_invalidUpload_shouldNotStoreOrSave() {
        CreateDocumentCommand command = new CreateDocumentCommand("", null);
        doThrow(new InvalidUploadException("Invalid title"))
                .when(documentValidationService).validateCreate(command, file);
        assertThatThrownBy(() -> documentService.createDocument(command, file))
                .isInstanceOf(InvalidUploadException.class);
        verifyNoInteractions(documentStorageService, documentRepository, tagRepository);
        verifyNoInteractions(documentEventPublisher);
    }

    @Test
    void createDocument_databaseFailure_shouldDeleteStoredFile() {
        when(currentUser.id()).thenReturn(ownerId);
        UUID storageId = UUID.randomUUID();
        RuntimeException failure = new IllegalStateException("Database unavailable");
        when(documentStorageService.store(file)).thenReturn(storageId);
        when(documentRepository.save(any(Document.class))).thenThrow(failure);
        assertThatThrownBy(() -> documentService.createDocument(new CreateDocumentCommand("Title", null), file))
                .isSameAs(failure);
        verify(documentStorageService).delete(storageId);
        verifyNoInteractions(documentEventPublisher);
    }

    @Test
    void createDocument_storageFailure_shouldNotSave() {
        when(documentStorageService.store(file)).thenThrow(new IllegalStateException("Disk full"));
        assertThatThrownBy(() -> documentService.createDocument(new CreateDocumentCommand("Title", null), file))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(documentRepository);
        verifyNoInteractions(documentEventPublisher);
    }

    @Test
    void getDocumentsByTag_none_shouldReturnUntaggedDocuments() {
        when(currentUser.id()).thenReturn(ownerId);
        Document document = new Document();
        DocumentDto dto = DocumentDto.builder().title("Untagged").build();
        when(documentRepository.findByOwnerIdAndTagIsNull(ownerId)).thenReturn(List.of(document));
        when(documentMapper.toDto(document)).thenReturn(dto);
        assertThat(documentService.getDocumentsByTag("none")).containsExactly(dto);
        verify(documentRepository, never()).findByOwnerIdAndTag_Name(any(), any());
    }

    @Test
    void createDocument_nonPdf_shouldNotPublishOcrJob() {
        CreateDocumentCommand command =
                new CreateDocumentCommand("Image", null);

        MockMultipartFile image = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        UUID storageId = UUID.randomUUID();

        when(currentUser.id()).thenReturn(ownerId);
        when(documentStorageService.store(image)).thenReturn(storageId);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> {
                    Document document = invocation.getArgument(0);
                    document.setId(UUID.randomUUID());
                    return document;
                });

        DocumentDto output = DocumentDto.builder()
                .title("Image")
                .build();

        when(documentMapper.toDto(any(Document.class)))
                .thenReturn(output);

        assertThat(documentService.createDocument(command, image))
                .isEqualTo(output);

        verify(documentRepository).save(any(Document.class));
        verifyNoInteractions(documentEventPublisher);
    }

    @Test
    void createDocument_publishFailure_shouldKeepStoredDocument() {
        CreateDocumentCommand command =
                new CreateDocumentCommand("Title", null);

        UUID documentId = UUID.randomUUID();
        UUID storageId = UUID.randomUUID();

        Document saved = Document.builder()
                .id(documentId)
                .title("Title")
                .originalFilename("file.pdf")
                .contentType("application/pdf")
                .storageUuid(storageId)
                .build();

        DocumentDto output = DocumentDto.builder()
                .id(documentId)
                .title("Title")
                .build();

        when(currentUser.id()).thenReturn(ownerId);
        when(documentStorageService.store(file)).thenReturn(storageId);
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(documentMapper.toDto(saved)).thenReturn(output);

        OcrJobPublishException failure = new OcrJobPublishException(
                documentId,
                new IllegalStateException("RabbitMQ unavailable")
        );

        doThrow(failure)
                .when(documentEventPublisher)
                .publishDocumentUploaded(any(DocumentUploadedMessage.class));

        assertThatThrownBy(
                () -> documentService.createDocument(command, file)
        ).isSameAs(failure);

        verify(documentRepository).save(any(Document.class));
        verify(documentStorageService, never()).delete(any());
        verify(documentRepository, never()).delete(any(Document.class));
    }
}
