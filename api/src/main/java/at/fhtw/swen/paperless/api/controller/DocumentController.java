package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.controller.request.CreateDocumentRequest;
import at.fhtw.swen.paperless.api.controller.request.UpdateDocumentTagRequest;
import at.fhtw.swen.paperless.api.controller.response.DocumentResponse;
import at.fhtw.swen.paperless.api.service.DocumentService;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request
    ) {
        log.debug("REST request to create document '{}'", request.title());

        DocumentDto dto = DocumentDto.builder()
                .title(request.title())
                .originalFilename(request.originalFilename())
                .contentType(request.contentType())
                .fileSize(request.fileSize())
//                .tagId(request.tagId())
                .build();

        DocumentDto created = documentService.createDocument(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        return documentService.getDocument(id)
                .map(dto -> ResponseEntity.ok(toResponse(dto)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(
            @RequestParam(required = false) String tag
    ) {
        List<DocumentDto> documents =
                tag == null
                        ? documentService.getAllDocuments()
                        : documentService.getDocumentsByTag(tag);

        return ResponseEntity.ok(
                documents.stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/tag")
    public ResponseEntity<DocumentResponse> updateDocumentTag(
            @PathVariable UUID id,
            @RequestBody UpdateDocumentTagRequest request
    ) {
        return documentService.updateDocumentTag(id, request.tagId())
                .map(dto -> ResponseEntity.ok(toResponse(dto)))
                .orElse(ResponseEntity.notFound().build());
    }

    private DocumentResponse toResponse(DocumentDto dto) {
        return DocumentResponse.builder()
                .id(dto.id())
                .title(dto.title())
                .originalFilename(dto.originalFilename())
                .contentType(dto.contentType())
                .fileSize(dto.fileSize())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .tagId(dto.tagId())
                .tagName(dto.tagName())
                .build();
    }
}