package at.fhtw.swen.paperless.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

@ConfigurationProperties(prefix = "paperless.upload")
public record UploadProperties(
        DataSize maxFileSize,
        int maxTitleLength,
        List<String> allowedContentTypes
) {
}
