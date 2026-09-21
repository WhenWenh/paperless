package at.fhtw.swen.paperless.api.controller.request;

import java.util.UUID;

public record UpdateDocumentTagRequest(
        UUID tagId
) {
}
