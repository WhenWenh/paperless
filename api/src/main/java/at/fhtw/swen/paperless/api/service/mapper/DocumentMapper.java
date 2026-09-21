package at.fhtw.swen.paperless.api.service.mapper;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentMapper {

    public DocumentDto toDto(Document entity) {
        UUID tagId = null;
        String tagName = null;

        if (entity.getTag() != null) {
            tagId = entity.getTag().getId();
            tagName = entity.getTag().getName();
        }

        return new DocumentDto(
                entity.getId(),
                entity.getTitle(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                tagId,
                tagName
        );
    }

    public Document toEntity(DocumentDto dto) {
        Document entity = new Document();
        entity.setTitle(dto.title());
        entity.setOriginalFilename(dto.originalFilename());
        entity.setContentType(dto.contentType());
        entity.setFileSize(dto.fileSize());
        return entity;
    }
}