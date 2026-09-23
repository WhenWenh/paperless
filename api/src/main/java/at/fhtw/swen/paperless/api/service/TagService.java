package at.fhtw.swen.paperless.api.service;

import at.fhtw.swen.paperless.api.service.dto.TagDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagService {
    TagDto createTag(String name);

    Optional<TagDto> getTagById(UUID id);

    Optional<TagDto> getTagByName(String name);

    List<TagDto> getAllTags();

    void deleteTag(UUID id);
}
