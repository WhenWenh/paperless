package at.fhtw.swen.paperless.api.service;

import at.fhtw.swen.paperless.api.service.dto.DocumentDto;

public interface DocumentService {

    DocumentDto createDocument(DocumentDto document);
}