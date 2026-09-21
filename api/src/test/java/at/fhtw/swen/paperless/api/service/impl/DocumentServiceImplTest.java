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

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    void createDocument_shouldPersistAndReturnDto() {
        DocumentDto input = new DocumentDto(
                null,
                "Title",
                "file.pdf",
                "application/pdf",
                123L,
                null,
                null, null, null
        );
        Document entity = new Document();
        Document saved = new Document();
        saved.setId(UUID.randomUUID());
        saved.setTitle("Title");
        DocumentDto output = new DocumentDto(
                saved.getId(),
                "Title",
                "file.pdf",
                "application/pdf",
                123L,
                null,
                null, null, null
        );
        when(documentMapper.toEntity(input)).thenReturn(entity);
        when(documentRepository.save(entity)).thenReturn(saved);
        when(documentMapper.toDto(saved)).thenReturn(output);

        DocumentDto result = documentService.createDocument(input);

        assertThat(result).isEqualTo(output);
        assertThat(result.id()).isNotNull();
        assertThat(result.title()).isEqualTo("Title");
        verify(documentRepository).save(entity);
    }

    @Test
    void getDocument_shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(documentService.getDocument(id)).isEmpty();
    }

    @Test
    void getAllDocuments_shouldReturnMappedList() {
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        DocumentDto dto = new DocumentDto(
                doc.getId(),
                "T",
                "f",
                "c",
                1L,
                null,
                null, null, null
        );
        when(documentRepository.findAll()).thenReturn(List.of(doc));
        when(documentMapper.toDto(doc)).thenReturn(dto);

        assertThat(documentService.getAllDocuments()).containsExactly(dto);
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
        DocumentDto input = new DocumentDto(null, "Title", "file.pdf", "application/pdf", 123L, null, null, tagId, null);
        Document entity = new Document();

        when(documentMapper.toEntity(input)).thenReturn(entity);
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(documentRepository.save(entity)).thenReturn(entity);

        documentService.createDocument(input);

        assertThat(entity.getTag()).isSameAs(tag);
    }

    @Test
    void createDocument_withMissingTag_shouldThrowAndNotSave() {
        UUID tagId = UUID.randomUUID();
        DocumentDto input = new DocumentDto(null, "Title", "file.pdf", "application/pdf", 123L, null, null, tagId, null);

        when(documentMapper.toEntity(input)).thenReturn(new Document());
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocument(input))
                .isInstanceOf(TagNotFoundException.class);
        verify(documentRepository, never()).save(any());
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
}
