package at.fhtw.swen.paperless.api.controller.response;

import lombok.Builder;

import java.util.UUID;

/**
 * Response returned for every tag-related endpoint.
 */
@Builder
public record TagResponse(
        UUID id,           // tag UUID
        String name        // tag name
) {
}
