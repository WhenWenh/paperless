package at.fhtw.swen.paperless.api.search;

import java.util.List;
import java.util.UUID;

public interface SearchService {

    List<UUID> search(
            String query,
            UUID ownerId
    );
}