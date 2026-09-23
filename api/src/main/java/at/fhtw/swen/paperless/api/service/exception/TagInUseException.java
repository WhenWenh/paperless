package at.fhtw.swen.paperless.api.service.exception;

import java.util.UUID;

public class TagInUseException extends RuntimeException {

    public TagInUseException(UUID id, long documentCount) {
        super("Tag " + id + " is still assigned to " + documentCount + " document(s)");
    }
}
