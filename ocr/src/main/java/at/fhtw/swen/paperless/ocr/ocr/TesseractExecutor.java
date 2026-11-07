package at.fhtw.swen.paperless.ocr.ocr;

import org.springframework.core.io.Resource;

public interface TesseractExecutor {

    String extractText(
            Resource document,
            String contentType
    );
}