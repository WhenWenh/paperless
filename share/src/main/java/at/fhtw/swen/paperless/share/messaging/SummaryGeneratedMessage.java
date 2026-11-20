package at.fhtw.swen.paperless.share.messaging;

import java.util.UUID;

public record SummaryGeneratedMessage(
        UUID documentId,
        String summary
) {
}