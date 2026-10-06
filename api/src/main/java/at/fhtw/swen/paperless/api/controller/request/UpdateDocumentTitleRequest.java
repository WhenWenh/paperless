package at.fhtw.swen.paperless.api.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDocumentTitleRequest(

        @NotBlank
        @Size(max = 20)
        String title
) {
}