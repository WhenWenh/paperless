package at.fhtw.swen.paperless.api.service;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagService {
    Tag createTag(String name);

    Optional<Tag> getTagById(UUID id);

    Optional<Tag> getTagByName(String name);

    List<Tag> getAllTags();

    void deleteTag(UUID id);
}
