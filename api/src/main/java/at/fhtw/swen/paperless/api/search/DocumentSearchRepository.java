package at.fhtw.swen.paperless.api.search;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentSearchRepository
        extends ElasticsearchRepository<DocumentSearchIndex, UUID> {

    List<DocumentSearchIndex> findByOwnerIdAndContentContaining(
            UUID ownerId,
            String content
    );
}