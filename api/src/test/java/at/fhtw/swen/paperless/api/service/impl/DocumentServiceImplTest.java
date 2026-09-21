package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import at.fhtw.swen.paperless.api.service.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

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
                null
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
                null
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

}
