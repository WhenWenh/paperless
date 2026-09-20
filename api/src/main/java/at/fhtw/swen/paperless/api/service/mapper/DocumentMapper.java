package at.fhtw.swen.paperless.api.service.mapper;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import at.fhtw.swen.paperless.api.service.dto.DocumentDto;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDto toDto(Document entity) {
        return new DocumentDto(
                entity.getId(),
                entity.getTitle(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
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