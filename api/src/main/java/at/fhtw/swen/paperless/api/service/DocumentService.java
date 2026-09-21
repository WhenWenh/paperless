package at.fhtw.swen.paperless.api.service;

import at.fhtw.swen.paperless.api.service.dto.DocumentDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentService {

    DocumentDto createDocument(DocumentDto document);
    List<DocumentDto> getAllDocuments();
    Optional<DocumentDto> getDocument(UUID id);
}