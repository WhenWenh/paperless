package at.fhtw.swen.paperless.genai.index;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

public interface DocumentIndexRepository
        extends ElasticsearchRepository<DocumentIndex, UUID> {
}