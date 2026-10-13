package at.fhtw.swen.paperless.ocr.messaging;

public class OcrProcessingException extends RuntimeException {

    public OcrProcessingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}