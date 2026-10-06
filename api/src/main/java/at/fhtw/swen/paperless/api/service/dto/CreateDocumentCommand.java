package at.fhtw.swen.paperless.api.service.dto;

import java.util.UUID;

public record CreateDocumentCommand(
        String title,
        UUID tagId
) {
}