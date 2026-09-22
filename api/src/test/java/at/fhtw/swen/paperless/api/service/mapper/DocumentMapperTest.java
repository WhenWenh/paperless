package at.fhtw.swen.paperless.api.service.mapper;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapper();

    @Test
    void toDto_shouldMapEntityToDto() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();

        Document document = Document.builder()
                .id(id)
                .title("Test Document")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // Act
        DocumentDto dto = mapper.toDto(document);

        // Assert
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.title()).isEqualTo("Test Document");
        assertThat(dto.originalFilename()).isEqualTo("test.pdf");
        assertThat(dto.contentType()).isEqualTo("application/pdf");
        assertThat(dto.fileSize()).isEqualTo(12345L);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toEntity_shouldMapDtoToEntity() {
        // Arrange
        DocumentDto dto = DocumentDto.builder()
                .id(UUID.randomUUID())
                .title("Test Document")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Act
        Document entity = mapper.toEntity(dto);

        // Assert
        assertThat(entity.getTitle()).isEqualTo("Test Document");
        assertThat(entity.getOriginalFilename()).isEqualTo("test.pdf");
        assertThat(entity.getContentType()).isEqualTo("application/pdf");
        assertThat(entity.getFileSize()).isEqualTo(12345L);

        // toEntity intentionally does not map persistence fields
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }


    @Test
    void toDto_shouldMapCollection() {
        Document first = Document.builder().title("First").build();
        Document second = Document.builder().title("Second").build();

        List<DocumentDto> result = mapper.toDto(List.of(first, second));

        assertThat(result)
                .extracting(DocumentDto::title)
                .containsExactly("First", "Second");
    }

    @Test
    void toDto_shouldReturnEmptyListForEmptyCollection() {
        List<Document> documents = List.of();

        assertThat(mapper.toDto(documents)).isEmpty();
    }
}
