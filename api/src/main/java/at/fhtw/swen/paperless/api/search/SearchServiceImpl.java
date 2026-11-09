package at.fhtw.swen.paperless.api.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final DocumentSearchRepository repository;

    @Override
    public List<UUID> search(
            String query,
            UUID ownerId
    ) {
        return repository
                .findByOwnerIdAndContentContaining(
                        ownerId,
                        query
                )
                .stream()
                .map(DocumentSearchIndex::getDocumentId)
                .toList();
    }
}