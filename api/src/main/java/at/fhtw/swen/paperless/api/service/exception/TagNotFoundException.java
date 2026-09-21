package at.fhtw.swen.paperless.api.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

// The old repo mapped this to 404 in its GlobalExceptionHandler.
// This repo has no global handler yet, so @ResponseStatus keeps the same behaviour.
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(UUID id) {
        super("Tag not found: " + id);
    }
}
