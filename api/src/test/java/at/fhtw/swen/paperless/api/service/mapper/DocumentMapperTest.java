package at.fhtw.swen.paperless.api.service.mapper;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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

        Document document = new Document();
        document.setId(id);
        document.setTitle("Test Document");
        document.setOriginalFilename("test.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(12345L);
        document.setCreatedAt(createdAt);
        document.setUpdatedAt(updatedAt);

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
        DocumentDto dto = new DocumentDto(
                UUID.randomUUID(),
                "Test Document",
                "test.pdf",
                "application/pdf",
                12345L,
                Instant.now(),
                Instant.now(), null, null
        );

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
    void toDto_shouldMapEntityWithTag() {
        UUID tagId = UUID.randomUUID();
        Tag tag = new Tag();
        tag.setId(tagId);
        tag.setName("Finance");

        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle("Test Document");
        document.setTag(tag);

        DocumentDto dto = mapper.toDto(document);

        assertThat(dto.tagId()).isEqualTo(tagId);
        assertThat(dto.tagName()).isEqualTo("Finance");
    }
}
