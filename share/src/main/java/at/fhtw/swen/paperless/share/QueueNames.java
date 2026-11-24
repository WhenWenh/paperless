package at.fhtw.swen.paperless.share;

public final class QueueNames {

    public static final String DOCUMENT_UPLOAD_QUEUE =
            "document.uploaded";

    public static final String OCR_COMPLETED_QUEUE =
            "document.ocr.completed";

    /**
     * Holds OCR results whose summary could not be generated.
     * The document itself is already indexed and stays usable;
     * only the summary is missing.
     */
    public static final String OCR_COMPLETED_DEAD_LETTER_QUEUE =
            "document.ocr.completed.dlq";

    public static final String SUMMARY_GENERATED_QUEUE =
            "document.summary.generated";

    private QueueNames() {}
}
