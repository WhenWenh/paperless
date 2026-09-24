package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.service.DocumentService;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import at.fhtw.swen.paperless.api.service.mapper.DocumentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final TagRepository tagRepository;

    @Override
    public DocumentDto createDocument(DocumentDto document) {
        log.debug("Creating document with title '{}'", document.title());

        Document entity = documentMapper.toEntity(document);

        if (document.tagId() != null) {
            Tag tag = tagRepository.findById(document.tagId())
                    .orElseThrow(() -> new TagNotFoundException(document.tagId()));
            entity.setTag(tag);
        }

        Document saved = documentRepository.save(entity);

        return documentMapper.toDto(saved);
    }

    @Override
    public List<DocumentDto> getAllDocuments() {
        return documentMapper.toDto(documentRepository.findAll());
    }

    @Override
    public Optional<DocumentDto> getDocument(UUID id) {
        return documentRepository.findById(id)
                .map(documentMapper::toDto);
    }

    @Override
    public void deleteDocument(UUID id) {
        log.debug("Deleting document with id '{}'", id);
        documentRepository.deleteById(id);
    }

    @Override
    public List<DocumentDto> getDocumentsByTag(String tagName) {
        return documentRepository.findByTag_Name(tagName).stream()
                .map(documentMapper::toDto)
                .toList();
    }

    @Override
    public Optional<DocumentDto> updateDocumentTag(UUID id, UUID tagId) {
        return documentRepository.findById(id)
                .map(document -> {
                    if (tagId == null) {
                        document.setTag(null);
                    } else {
                        Tag tag = tagRepository.findById(tagId)
                                .orElseThrow(() -> new TagNotFoundException(tagId));
                        document.setTag(tag);
                    }
                    return documentMapper.toDto(documentRepository.save(document));
                });
    }
}
