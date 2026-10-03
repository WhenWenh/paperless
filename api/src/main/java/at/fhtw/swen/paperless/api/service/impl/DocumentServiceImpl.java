package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.persistence.repository.TagRepository;
import at.fhtw.swen.paperless.api.service.DocumentService;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import at.fhtw.swen.paperless.api.service.exception.TagNotFoundException;
import at.fhtw.swen.paperless.api.service.mapper.DocumentMapper;
import at.fhtw.swen.paperless.api.service.DocumentStorageService;
import at.fhtw.swen.paperless.api.service.DocumentValidationService;
import at.fhtw.swen.paperless.api.service.dto.CreateDocumentCommand;
import org.springframework.web.multipart.MultipartFile;

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
    private final DocumentStorageService documentStorageService;
    private final DocumentValidationService documentValidationService;

    @Override
    public DocumentDto createDocument(
            CreateDocumentCommand command,
            MultipartFile file
    ) {
        log.debug(
                "Creating document with title '{}'",
                command.title()
        );

        documentValidationService.validateCreate(command, file);

        UUID storageUuid = documentStorageService.store(file);

        try {
            Document entity = new Document();

            entity.setTitle(command.title());
            entity.setOriginalFilename(file.getOriginalFilename());
            entity.setContentType(file.getContentType());
            entity.setFileSize(file.getSize());
            entity.setStorageUuid(storageUuid);

            if (command.tagId() != null) {
                Tag tag = tagRepository.findById(command.tagId())
                        .orElseThrow(
                                () -> new TagNotFoundException(command.tagId())
                        );

                entity.setTag(tag);
            }

            Document saved = documentRepository.save(entity);

            return documentMapper.toDto(saved);
        } catch (RuntimeException exception) {
            documentStorageService.delete(storageUuid);
            throw exception;
        }
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
        if ("none".equals(tagName)) {
            return documentRepository.findByTagIsNull().stream()
                    .map(documentMapper::toDto)
                    .toList();
        }

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
