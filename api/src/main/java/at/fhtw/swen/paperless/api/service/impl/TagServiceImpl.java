package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;

    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public Tag createTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        return tagRepository.save(tag);
    }

    @Override
    public Optional<Tag> getTagById(UUID id) {
        return tagRepository.findById(id);
    }

    @Override
    public Optional<Tag> getTagByName(String name) {
        return tagRepository.findByName(name);
    }

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    public void deleteTag(UUID id) {
        tagRepository.deleteById(id);
    }
}
