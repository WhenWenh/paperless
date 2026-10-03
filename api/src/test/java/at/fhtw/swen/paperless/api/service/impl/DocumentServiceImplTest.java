package at.fhtw.swen.paperless.api.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import at.fhtw.swen.paperless.api.service.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import at.fhtw.swen.paperless.api.service.DocumentStorageService;
import at.fhtw.swen.paperless.api.service.DocumentValidationService;
import at.fhtw.swen.paperless.api.service.dto.CreateDocumentCommand;
import at.fhtw.swen.paperless.api.service.exception.InvalidUploadException;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private TagRepository tagRepository;

    @Mock private DocumentStorageService documentStorageService;
    @Mock private DocumentValidationService documentValidationService;

    private final MockMultipartFile file =
            new MockMultipartFile("file", "file.pdf", "application/pdf", new byte[]{1, 2, 3});

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    void createDocument_shouldPersistFileMetadataAndReturnDto() {
        CreateDocumentCommand command = new CreateDocumentCommand("Title", null);
        UUID storageId = UUID.randomUUID();
        Document saved = Document.builder().id(UUID.randomUUID()).build();
        DocumentDto output = DocumentDto.builder().id(saved.getId()).title("Title").build();
        when(documentStorageService.store(file)).thenReturn(storageId);
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(documentMapper.toDto(saved)).thenReturn(output);

        assertThat(documentService.createDocument(command, file)).isEqualTo(output);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        Document entity = captor.getValue();
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getOriginalFilename()).isEqualTo("file.pdf");
        assertThat(entity.getContentType()).isEqualTo("application/pdf");
        assertThat(entity.getFileSize()).isEqualTo(3L);
        assertThat(entity.getStorageUuid()).isEqualTo(storageId);
        assertThat(entity.getTag()).isNull();
        verify(documentValidationService).validateCreate(command, file);
        verify(documentStorageService, never()).delete(any());
    }
    @Test
    void getDocument_shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(documentService.getDocument(id)).isEmpty();
    }

    @Test
    void getAllDocuments_shouldReturnMappedList() {
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
        List<DocumentDto> dtos = List.of(dto);

        when(documentRepository.findAll()).thenReturn(documents);
        when(documentMapper.toDto(documents)).thenReturn(dtos);

        assertThat(documentService.getAllDocuments())
                .containsExactly(dto);

        verify(documentMapper).toDto(documents);
    }

    @Test
    void deleteDocument_shouldInvokeRepository() {
        UUID id = UUID.randomUUID();
        documentService.deleteDocument(id);
        verify(documentRepository).deleteById(id);
    }

    @Test
    void createDocument_withTag_shouldAttachTag() {
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
        UUID tagId = UUID.randomUUID();
        CreateDocumentCommand input = new CreateDocumentCommand("Title", tagId);

        UUID storageId = UUID.randomUUID();
        when(documentStorageService.store(file)).thenReturn(storageId);
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocument(input, file))
                .isInstanceOf(TagNotFoundException.class);
        verify(documentRepository, never()).save(any());
        verify(documentStorageService).delete(storageId);
    }

    @Test
    void getDocumentsByTag_shouldReturnDocuments() {
        Document doc = new Document();
        DocumentDto dto = new DocumentDto(UUID.randomUUID(), "T", "f", "c", 1L, null, null, null, "Finance");

        when(documentRepository.findByTag_Name("Finance")).thenReturn(List.of(doc));
        when(documentMapper.toDto(doc)).thenReturn(dto);

        assertThat(documentService.getDocumentsByTag("Finance")).containsExactly(dto);
    }

    @Test
    void updateDocumentTag_withNull_shouldRemoveTag() {
        UUID id = UUID.randomUUID();
        Document doc = new Document();
        doc.setTag(new Tag());

        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));
        when(documentRepository.save(doc)).thenReturn(doc);
        when(documentMapper.toDto(doc)).thenReturn(
                new DocumentDto(id, "T", "f", "c", 1L, null, null, null, null));

        assertThat(documentService.updateDocumentTag(id, null)).isPresent();
        assertThat(doc.getTag()).isNull();
    }

    @Test
    void updateDocumentTag_withTag_shouldAssignTag() {
        UUID id = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        Document doc = new Document();
        Tag tag = new Tag();
        tag.setId(tagId);

        when(documentRepository.findById(id)).thenReturn(Optional.of(doc));
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(documentRepository.save(doc)).thenReturn(doc);
        when(documentMapper.toDto(doc)).thenReturn(
                new DocumentDto(id, "T", "f", "c", 1L, null, null, tagId, null));

        assertThat(documentService.updateDocumentTag(id, tagId)).isPresent();
        assertThat(doc.getTag()).isSameAs(tag);
    }

    @Test
    void updateDocumentTag_missingTag_shouldThrow() {
        UUID id = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        when(documentRepository.findById(id)).thenReturn(Optional.of(new Document()));
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.updateDocumentTag(id, tagId))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void updateDocumentTag_missingDocument_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

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
    }

    @Test
    void createDocument_databaseFailure_shouldDeleteStoredFile() {
        UUID storageId = UUID.randomUUID();
        RuntimeException failure = new IllegalStateException("Database unavailable");
        when(documentStorageService.store(file)).thenReturn(storageId);
        when(documentRepository.save(any(Document.class))).thenThrow(failure);
        assertThatThrownBy(() -> documentService.createDocument(new CreateDocumentCommand("Title", null), file))
                .isSameAs(failure);
        verify(documentStorageService).delete(storageId);
    }

    @Test
    void createDocument_storageFailure_shouldNotSave() {
        when(documentStorageService.store(file)).thenThrow(new IllegalStateException("Disk full"));
        assertThatThrownBy(() -> documentService.createDocument(new CreateDocumentCommand("Title", null), file))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(documentRepository);
    }

    @Test
    void getDocumentsByTag_none_shouldReturnUntaggedDocuments() {
        Document document = new Document();
        DocumentDto dto = DocumentDto.builder().title("Untagged").build();
        when(documentRepository.findByTagIsNull()).thenReturn(List.of(document));
        when(documentMapper.toDto(document)).thenReturn(dto);
        assertThat(documentService.getDocumentsByTag("none")).containsExactly(dto);
        verify(documentRepository, never()).findByTag_Name(any());
    }
}