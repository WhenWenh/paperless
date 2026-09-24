package at.fhtw.swen.paperless.api.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload used when a client creates a new tag.
 * Only the `name` field is required.
 */
public record CreateTagRequest(
        @NotBlank
        @Size(max = 50)
        String name
) {}
