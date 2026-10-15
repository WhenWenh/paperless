package at.fhtw.swen.paperless.api.service.exception;

import java.util.UUID;

public class OcrJobPublishException extends RuntimeException {

    public OcrJobPublishException(
            UUID documentId,
            Throwable cause
    ) {
        super(
                "Document " + documentId
                        + " was saved, but an error occurred "
                        + "while publishing its OCR job.",
                cause
        );
    }
}