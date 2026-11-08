package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.security.CurrentUser;
import at.fhtw.swen.paperless.api.service.DocumentStorageService;
import at.fhtw.swen.paperless.api.service.DocumentValidationService;
import at.fhtw.swen.paperless.api.service.exception.DocumentNotFoundException;
import at.fhtw.swen.paperless.api.service.impl.DocumentServiceImpl;
import at.fhtw.swen.paperless.api.service.mapper.DocumentMapper;

import at.fhtw.swen.paperless.api.messaging.DocumentEventPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.UUID;
import java.util.List;
import at.fhtw.swen.paperless.api.search.SearchService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "storage.type=filesystem"
})
class DocumentOwnershipTest {
    @Autowired private DocumentRepository documents;
    @Autowired private TagRepository tags;

    private final UUID owner = UUID.randomUUID();
    private final UUID otherOwner = UUID.randomUUID();
    private final DocumentStorageService storage = mock(DocumentStorageService.class);
    private final SearchService searchService = mock(SearchService.class);
    private DocumentServiceImpl service;
    private Document ownDocument;
    private Document foreignDocument;

    @BeforeEach
    void setUp() {
        CurrentUser user = mock(CurrentUser.class);
        when(user.id()).thenReturn(owner);

        service = new DocumentServiceImpl(
                documents,
                new DocumentMapper(),
                storage,
                mock(DocumentValidationService.class),
                tags,
                user,
                mock(DocumentEventPublisher.class),
                searchService
        );

        ownDocument = saveDocument(owner, "Own");
        foreignDocument = saveDocument(otherOwner, "Foreign");
    }

    @Test
    void listsAndLookups_shouldOnlyExposeOwnDocuments() {
        assertThat(service.getAllDocuments()).extracting(dto -> dto.id())
                .containsExactly(ownDocument.getId());
        assertThat(service.getDocument(ownDocument.getId())).isPresent();
        assertThat(service.getDocument(foreignDocument.getId())).isEmpty();
        assertThat(service.getDocumentsByTag("none")).extracting(dto -> dto.id())
                .containsExactly(ownDocument.getId());
        assertThat(documents.countDocumentsWithoutTag(owner)).isEqualTo(1);
    }

    @Test
    void tagFilterAndStatistics_shouldExcludeOtherOwners() {
        Tag tag = new Tag();
        tag.setName("Finance");
        tags.saveAndFlush(tag);
        ownDocument.setTag(tag);
        foreignDocument.setTag(tag);
        documents.flush();

        assertThat(service.getDocumentsByTag("Finance")).extracting(dto -> dto.id())
                .containsExactly(ownDocument.getId());
        assertThat(documents.getTagStatistics(owner)).singleElement().satisfies(stat -> {
            assertThat(stat.tagId()).isEqualTo(tag.getId());
            assertThat(stat.documentCount()).isEqualTo(1);
        });
        assertThat(documents.countDocumentsWithoutTag(owner)).isZero();
    }

    @Test
    void foreignDocument_shouldNotBeChangedOrDeleted() {
        UUID id = foreignDocument.getId();
        assertThatThrownBy(() -> service.updateDocumentTitle(id, "Changed"))
                .isInstanceOf(DocumentNotFoundException.class);
        assertThat(service.updateDocumentTag(id, null)).isEmpty();
        assertThatThrownBy(() -> service.deleteDocument(id))
                .isInstanceOf(DocumentNotFoundException.class);

        assertThat(documents.findById(id)).isPresent().get()
                .extracting(Document::getTitle).isEqualTo("Foreign");
        verifyNoInteractions(storage);
    }

    @Test
    void ownDocument_shouldAllowTitleUpdateAndDeletion() {
        service.updateDocumentTitle(ownDocument.getId(), "Changed");
        assertThat(documents.findById(ownDocument.getId()).orElseThrow().getTitle())
                .isEqualTo("Changed");
        service.deleteDocument(ownDocument.getId());
        assertThat(documents.findById(ownDocument.getId())).isEmpty();
        assertThat(documents.findById(foreignDocument.getId())).isPresent();
        verify(storage).delete(ownDocument.getStorageUuid());
    }

    private Document saveDocument(UUID ownerId, String title) {
        Document document = Document.builder().ownerId(ownerId).title(title)
                .originalFilename("test.pdf").contentType("application/pdf")
                .fileSize(10L).storageUuid(UUID.randomUUID()).build();
        return documents.saveAndFlush(document);
    }

    @Test
    void searchDocuments_shouldExcludeForeignAndMissingDocuments() {
        when(searchService.search("Rechnung", owner)).thenReturn(List.of(
                ownDocument.getId(), foreignDocument.getId(), UUID.randomUUID()
        ));

        assertThat(service.searchDocuments("Rechnung", null, false))
                .extracting(dto -> dto.id())
                .containsExactly(ownDocument.getId());
    }

    @Test
    void searchDocuments_withoutMatches_shouldNotReturnOtherDocuments() {
        when(searchService.search("missing", owner)).thenReturn(List.of());

        assertThat(service.searchDocuments("missing", null, false)).isEmpty();
    }
}
