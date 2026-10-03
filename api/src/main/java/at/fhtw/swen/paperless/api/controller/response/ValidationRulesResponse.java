package at.fhtw.swen.paperless.api.controller.response;

import java.util.List;

public record ValidationRulesResponse(
        int maxTitleLength,
        long maxFileSize,
        List<String> allowedContentTypes
) {
}