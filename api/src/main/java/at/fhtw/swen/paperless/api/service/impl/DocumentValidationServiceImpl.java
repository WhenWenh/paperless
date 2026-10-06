package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.config.UploadProperties;
import at.fhtw.swen.paperless.api.service.DocumentValidationService;
import at.fhtw.swen.paperless.api.service.dto.CreateDocumentCommand;
import lombok.RequiredArgsConstructor;
import at.fhtw.swen.paperless.api.service.exception.InvalidUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentValidationServiceImpl
        implements DocumentValidationService {

    private final UploadProperties uploadProperties;

    @Override
    public void validateCreate(
            CreateDocumentCommand command,
            MultipartFile file
    ) {
        validateTitle(command.title());
        validateFile(file);
    }

    @Override
    public void validateReplacement(MultipartFile file) {
        validateFile(file);
    }

    @Override
    public void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidUploadException(
                    "Document title is required."
            );
        }

        if (title.length() > uploadProperties.maxTitleLength()) {
            throw new InvalidUploadException(
                    "Document title must not exceed "
                            + uploadProperties.maxTitleLength()
                            + " characters."
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException(
                    "A file must be uploaded."
            );
        }

        if (file.getSize() > uploadProperties.maxFileSize().toBytes()) {
            throw new InvalidUploadException(
                    "The uploaded file exceeds the maximum allowed size of "
                            + uploadProperties.maxFileSize() + "."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !uploadProperties.allowedContentTypes()
                .contains(contentType)) {
            throw new InvalidUploadException(
                    "Unsupported content type: " + contentType
            );
        }
    }
}
