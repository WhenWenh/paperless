package at.fhtw.swen.paperless.api.service.exception;

public class TagAlreadyExistsException extends RuntimeException {

    public TagAlreadyExistsException(String name) {
        super("Tag already exists: " + name);
    }
}
