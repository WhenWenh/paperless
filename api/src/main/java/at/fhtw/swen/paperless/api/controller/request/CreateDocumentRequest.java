package at.fhtw.swen.paperless.api.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDocumentRequest(
        @NotBlank
        @Size(max = 20)
        String title,

        UUID tagId
) {
}