package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.persistence.repository.DocumentRepository;
import at.fhtw.swen.paperless.api.service.DocumentService;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
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

    @Override
    public DocumentDto createDocument(DocumentDto document) {
        log.debug("Creating document with title '{}'", document.title());

        Document entity = documentMapper.toEntity(document);
        Document saved = documentRepository.save(entity);

        return documentMapper.toDto(saved);
    }

    @Override
    public List<DocumentDto> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(documentMapper::toDto)
                .toList();
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
}