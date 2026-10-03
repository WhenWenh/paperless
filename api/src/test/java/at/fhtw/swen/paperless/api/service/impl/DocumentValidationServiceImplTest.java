package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.config.UploadProperties;
import at.fhtw.swen.paperless.api.service.dto.CreateDocumentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import at.fhtw.swen.paperless.api.service.exception.InvalidUploadException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class DocumentValidationServiceImplTest {

    private DocumentValidationServiceImpl validationService;

    @BeforeEach
    void setup() {
        UploadProperties properties =
                new UploadProperties(
                        DataSize.ofMegabytes(100),
                        20,
                        List.of(
                                "application/pdf",
                                "image/jpeg",
                                "image/png",
                                "text/plain"
                        )
                );
        validationService = new DocumentValidationServiceImpl(properties);
    }


    @Test
    void validateCreate_shouldAcceptValidDocument() {
        CreateDocumentCommand command =
                new CreateDocumentCommand(
                        "Invoice",
                        null
                );

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "invoice.pdf",
                        "application/pdf",
                        "content".getBytes()
                );

        assertThatCode(() ->
                validationService.validateCreate(
                        command,
                        file
                )
        ).doesNotThrowAnyException();
    }


    @Test
    void validateCreate_shouldRejectBlankTitle() {
        CreateDocumentCommand command =
                new CreateDocumentCommand(
                        "",
                        null
                );

        MockMultipartFile file =
                validPdf();

        assertThatThrownBy(() ->
                validationService.validateCreate(
                        command,
                        file
                )
        )
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining(
                        "Document title is required"
                );
    }


    @Test
    void validateTitle_shouldRejectTooLongTitle() {
        assertThatThrownBy(() ->
                validationService.validateTitle(
                        "123456789012345678901"
                )
        )
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining(
                        "must not exceed"
                );
    }


    @Test
    void validateCreate_shouldRejectMissingFile() {
        CreateDocumentCommand command =
                new CreateDocumentCommand(
                        "Title",
                        null
                );

        assertThatThrownBy(() ->
                validationService.validateCreate(
                        command,
                        null
                )
        )
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining(
                        "file must be uploaded"
                );
    }


    @Test
    void validateCreate_shouldRejectEmptyFile() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "empty.pdf",
                        "application/pdf",
                        new byte[0]
                );

        assertThatThrownBy(() ->
                validationService.validateReplacement(file)
        )
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining(
                        "file must be uploaded"
                );
    }


    @Test
    void validateCreate_shouldRejectUnsupportedContentType() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "script.exe",
                        "application/x-msdownload",
                        "content".getBytes()
                );

        assertThatThrownBy(() ->
                validationService.validateReplacement(file)
        )
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining(
                        "Unsupported content type"
                );
    }


    @Test
    void validateCreate_shouldRejectFileTooLarge() {
        UploadProperties properties =
                new UploadProperties(
                        DataSize.ofBytes(5),
                        20,
                        List.of(
                                "application/pdf"
                        )
                );

        DocumentValidationServiceImpl service = new DocumentValidationServiceImpl(properties);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "large.pdf",
                        "application/pdf",
                        "123456".getBytes()
                );


        assertThatThrownBy(() ->
                service.validateReplacement(file)
        )
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining(
                        "exceeds"
                );
    }


    @Test
    void validateReplacement_shouldUseSameValidationRules() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "image.png",
                        "image/png",
                        "content".getBytes()
                );

        assertThatCode(() ->
                validationService.validateReplacement(file)
        )
                .doesNotThrowAnyException();
    }


    private MockMultipartFile validPdf() {

        return new MockMultipartFile(
                "file",
                "file.pdf",
                "application/pdf",
                "content".getBytes()
        );
    }
    @Test
    void validateCreate_shouldAcceptExactLimits() {
        var service = new DocumentValidationServiceImpl(
                new UploadProperties(DataSize.ofBytes(5), 20, List.of("application/pdf")));
        var file = new MockMultipartFile("file", "limit.pdf", "application/pdf", new byte[5]);
        assertThatCode(() -> service.validateCreate(new CreateDocumentCommand("x".repeat(20), null), file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateTitle_shouldRejectNullAndWhitespace() {
        assertThatThrownBy(() -> validationService.validateTitle(null))
                .isInstanceOf(InvalidUploadException.class);
        assertThatThrownBy(() -> validationService.validateTitle("   "))
                .isInstanceOf(InvalidUploadException.class);
    }

    @Test
    void validateReplacement_shouldRejectMissingContentType() {
        var file = new MockMultipartFile("file", "file.pdf", null, new byte[]{1});
        assertThatThrownBy(() -> validationService.validateReplacement(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("Unsupported content type");
    }
}