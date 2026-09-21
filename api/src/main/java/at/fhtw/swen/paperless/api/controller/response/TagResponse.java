package at.fhtw.swen.paperless.api.controller.response;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;

import java.util.UUID;

/**
 * Response returned for every tag-related endpoint.
 */
public record TagResponse(
        UUID id,           // tag UUID
        String name        // tag name
) {
    /** Convenience factory for OpenAPI examples */
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
