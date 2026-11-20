package at.fhtw.swen.paperless.genai.service.exception;

public class GenaiException extends RuntimeException {

    public GenaiException(String message) {
        super(message);
    }
}