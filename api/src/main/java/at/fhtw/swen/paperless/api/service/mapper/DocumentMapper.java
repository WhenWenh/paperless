package at.fhtw.swen.paperless.api.service.mapper;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentMapper extends AbstractMapper<Document, DocumentDto> {

    @Override
    public DocumentDto toDto(Document entity) {
        UUID tagId = null;
        String tagName = null;

//        if (entity.getTag() != null) {
//            tagId = entity.getTag().getId();
//            tagName = entity.getTag().getName();
//        }

        return DocumentDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .originalFilename(entity.getOriginalFilename())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
//                .tagId(tagId)
//                .tagName(tagName)
                .build();
    }

    @Override
    public Document toEntity(DocumentDto dto) {
        return Document.builder()
                .title(dto.title())
                .originalFilename(dto.originalFilename())
                .contentType(dto.contentType())
                .fileSize(dto.fileSize())
                .build();
    }
}