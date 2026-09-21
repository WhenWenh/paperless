package at.fhtw.swen.paperless.api.controller.request;

/**
 * Payload used when a client creates a new tag.
 * Only the `name` field is required.
 */
public record CreateTagRequest(
        String name
) {}
