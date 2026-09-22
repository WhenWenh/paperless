package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.service.TagService;
import at.fhtw.swen.paperless.api.service.dto.TagDto;
import at.fhtw.swen.paperless.api.service.exception.TagAlreadyExistsException;
import at.fhtw.swen.paperless.api.service.exception.TagInUseException;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import at.fhtw.swen.paperless.api.service.mapper.TagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagServiceImpl(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    @Override
    public TagDto createTag(String name) {
        String trimmedName = name.trim();

        if (tagRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new TagAlreadyExistsException(trimmedName);
        }

        Tag tag = Tag.builder()
                .name(trimmedName)
                .build();

        TagDto created = tagMapper.mapToDto(tagRepository.save(tag));
        log.info("Created tag '{}' with id '{}'", created.getName(), created.getId());
        return created;
    }

    @Override
    public Optional<TagDto> getTagById(UUID id) {
        return tagRepository.findById(id).map(tagMapper::mapToDto);
    }

    @Override
    public Optional<TagDto> getTagByName(String name) {
        return tagRepository.findByName(name).map(tagMapper::mapToDto);
    }

    @Override
    public List<TagDto> getAllTags() {
        return tagMapper.mapToDto(tagRepository.findAll());
    }

    @Override
    public void deleteTag(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new TagNotFoundException(id);
        }

        long documentCount = tagRepository.countDocumentsByTagId(id);
        if (documentCount > 0) {
            throw new TagInUseException(id, documentCount);
        }

        tagRepository.deleteById(id);
        log.info("Deleted tag with id '{}'", id);
    }
}
