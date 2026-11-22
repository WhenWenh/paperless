package at.fhtw.swen.paperless.genai.service.exception;

/**
 * Raised when a summary cannot be produced by the GenAI provider.
 * The document itself stays indexed and usable; only the summary
 * is missing.
 */
public class GenaiException extends RuntimeException {

    public GenaiException(String message) {
        super(message);
    }

    public GenaiException(String message, Throwable cause) {
        super(message, cause);
    }
}
