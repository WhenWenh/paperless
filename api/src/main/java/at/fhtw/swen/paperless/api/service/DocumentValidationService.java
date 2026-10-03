package at.fhtw.swen.paperless.api.service;

import at.fhtw.swen.paperless.api.service.dto.CreateDocumentCommand;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentValidationService {

    void validateCreate(
            CreateDocumentCommand command,
            MultipartFile file
    );

    void validateReplacement(MultipartFile file);

    void validateTitle(String title);
}