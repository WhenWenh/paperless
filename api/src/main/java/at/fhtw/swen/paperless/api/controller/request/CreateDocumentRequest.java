package at.fhtw.swen.paperless.api.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDocumentRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 512)
        String originalFilename,

        @NotBlank
        @Size(max = 127)
        String contentType,

        @NotNull
        @PositiveOrZero
        Long fileSize,

        UUID tagId

) {
}