package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "storage.type=filesystem"
})
class DocumentRepositoryTest {
    @Autowired private DocumentRepository documentRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void updateSummaryById_shouldPersistSummaryForOnlyTheMatchingDocument() {
        UUID ownerId = UUID.randomUUID();
        Document target = documentRepository.saveAndFlush(document(ownerId, "Invoice"));
        Document other = documentRepository.saveAndFlush(document(ownerId, "Other document"));
        Instant updatedAt = Instant.parse("2026-11-21T12:00:00Z");

        int updated = documentRepository.updateSummaryById(
                target.getId(), "Die Rechnung beträgt 120 Euro.", updatedAt
        );
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        Document found = documentRepository.findById(target.getId()).orElseThrow();
        assertThat(found.getSummary()).isEqualTo("Die Rechnung beträgt 120 Euro.");
        assertThat(found.getTitle()).isEqualTo("Invoice");
        assertThat(found.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(documentRepository.findById(other.getId()).orElseThrow().getSummary())
                .isNull();
    }

    @Test
    void updateSummaryById_shouldReturnZeroForMissingDocument() {
        assertThat(documentRepository.updateSummaryById(
                UUID.randomUUID(), "Summary", Instant.now()
        )).isZero();
    }

    private Document document(UUID ownerId, String title) {
        return Document.builder()
                .ownerId(ownerId)
                .title(title)
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .build();
    }

    @Test
    void saveAndFind_shouldPersistMetadataAndGenerateIdAndTimestamps() {
        Document document = Document.builder().ownerId(java.util.UUID.randomUUID())
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
        Document doc = Document.builder().ownerId(java.util.UUID.randomUUID())
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
