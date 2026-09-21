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
        Document document = new Document();
        document.setTitle("Test");
        document.setOriginalFilename("test.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(100L);

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
        Document doc = new Document();
        doc.setTitle("ToDelete");
        doc.setOriginalFilename("d.pdf");
        doc.setContentType("application/pdf");
        doc.setFileSize(1L);
        Document saved = documentRepository.save(doc);

        documentRepository.deleteById(saved.getId());
        assertThat(documentRepository.findById(saved.getId())).isEmpty();
    }
}
