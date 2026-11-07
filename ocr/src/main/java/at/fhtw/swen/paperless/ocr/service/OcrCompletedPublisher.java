package at.fhtw.swen.paperless.ocr.service;

import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;

public interface OcrCompletedPublisher {

    void publish(OcrCompletedMessage result);
}