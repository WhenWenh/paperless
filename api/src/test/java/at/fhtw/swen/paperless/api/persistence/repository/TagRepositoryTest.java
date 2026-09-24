package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class TagRepositoryTest {
    @Autowired private TagRepository tagRepository;
    @Autowired private DocumentRepository documentRepository;

    @Test
    void existsByNameIgnoreCase_shouldMatchRegardlessOfCase() {
        tagRepository.saveAndFlush(Tag.builder().name("Rechnungen").build());

        assertThat(tagRepository.existsByNameIgnoreCase("rechnungen")).isTrue();
        assertThat(tagRepository.existsByNameIgnoreCase("RECHNUNGEN")).isTrue();
        assertThat(tagRepository.existsByNameIgnoreCase("Verträge")).isFalse();
    }

    @Test
    void countDocumentsByTagId_shouldCountOnlyDocumentsWithThatTag() {
        Tag used = tagRepository.save(Tag.builder().name("Rechnungen").build());
        Tag unused = tagRepository.save(Tag.builder().name("Verträge").build());
        documentRepository.save(document("A", used));
        documentRepository.save(document("B", used));
        documentRepository.save(document("C", null));
        documentRepository.flush();

        assertThat(tagRepository.countDocumentsByTagId(used.getId())).isEqualTo(2);
        assertThat(tagRepository.countDocumentsByTagId(unused.getId())).isZero();
    }

    private Document document(String title, Tag tag) {
        Document document = new Document();
        document.setTitle(title);
        document.setOriginalFilename(title + ".pdf");
        document.setContentType("application/pdf");
        document.setFileSize(1L);
        document.setTag(tag);
        return document;
    }
}
