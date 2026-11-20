package at.fhtw.swen.paperless.genai.service;

import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;

public interface SummaryGenerator {

    String generate(OcrCompletedMessage message);
}