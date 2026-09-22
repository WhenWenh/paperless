package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class DocumentRepositoryTest {
    @Autowired private DocumentRepository documentRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void saveAndFind_shouldPersistMetadataAndGenerateIdAndTimestamps() {
        Document document = Document.builder()
                .title("Test")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .build();

        Document saved = documentRepository.saveAndFlush(document);
        var id = saved.getId();
        assertThat(id).isNotNull();
        entityManager.clear(); // Force a database read instead of returning the managed entity.

        Document found = documentRepository.findById(id).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Test");
        assertThat(found.getOriginalFilename()).isEqualTo("test.pdf");
        assertThat(found.getContentType()).isEqualTo("application/pdf");
        assertThat(found.getFileSize()).isEqualTo(100L);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
    }

    @Test
    void delete_shouldRemoveDocument() {
        Document doc = Document.builder()
                .title("ToDelete")
                .originalFilename("d.pdf")
                .contentType("application/pdf")
                .fileSize(1L)
                .build();
        Document saved = documentRepository.save(doc);

        documentRepository.deleteById(saved.getId());
        assertThat(documentRepository.findById(saved.getId())).isEmpty();
    }
}
