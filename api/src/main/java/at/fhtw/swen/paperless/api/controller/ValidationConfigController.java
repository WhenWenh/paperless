package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.config.UploadProperties;
import at.fhtw.swen.paperless.api.controller.response.ValidationRulesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ValidationConfigController {

    private final UploadProperties properties;

    @GetMapping("/validation")
    public ValidationRulesResponse validationRules() {
        return new ValidationRulesResponse(
                properties.maxTitleLength(),
                properties.maxFileSize().toBytes(),
                properties.allowedContentTypes()
        );
    }
}