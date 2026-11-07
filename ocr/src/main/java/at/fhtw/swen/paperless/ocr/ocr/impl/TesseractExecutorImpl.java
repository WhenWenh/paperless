package at.fhtw.swen.paperless.ocr.ocr.impl;

import at.fhtw.swen.paperless.ocr.ocr.CommandExecutor;
import at.fhtw.swen.paperless.ocr.ocr.TesseractExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
@Service
public class TesseractExecutorImpl implements TesseractExecutor {

    private final CommandExecutor commandExecutor;

    public TesseractExecutorImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String extractText(Resource document, String contentType) {
        Path tempDirectory = null;

        try {
            String normalizedContentType =
                    contentType.toLowerCase(Locale.ROOT);

            tempDirectory = Files.createTempDirectory(
                    "paperless-ocr-"
            );

            Path inputFile = tempDirectory.resolve(
                    "input" + extensionFor(normalizedContentType)
            );

            try (InputStream input = document.getInputStream()) {
                Files.copy(
                        input,
                        inputFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            if ("application/pdf".equals(normalizedContentType)) {
                return extractFromPdf(tempDirectory, inputFile);
            }

            return extractFromImage(inputFile);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "OCR processing failed",
                    exception
            );

        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    private String extractFromPdf(
            Path directory,
            Path pdf
    ) throws IOException {
        commandExecutor.execute(
                "pdftoppm",
                "-png",
                pdf.toString(),
                directory.resolve("page").toString()
        );

        List<Path> pages;

        try (Stream<Path> files = Files.list(directory)) {
            pages = files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();

                        return name.startsWith("page-")
                                && name.endsWith(".png");
                    })
                    .sorted(
                            Comparator.comparingInt(
                                    TesseractExecutorImpl::pageNumber
                            )
                    )
                    .toList();
        }

        if (pages.isEmpty()) {
            throw new IllegalStateException(
                    "PDF conversion produced no page images"
            );
        }

        StringBuilder result = new StringBuilder();

        for (Path page : pages) {
            result.append(extractFromImage(page));
            result.append("\n");
        }

        return result.toString();
    }

    private String extractFromImage(Path image) throws IOException {
        return commandExecutor.execute(
                "tesseract",
                image.toString(),
                "stdout"
        );
    }

    private static int pageNumber(Path page) {
        String name = page.getFileName().toString();

        String number = name.substring(
                "page-".length(),
                name.length() - ".png".length()
        );

        return Integer.parseInt(number);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/tiff" -> ".tiff";
            case "image/bmp" -> ".bmp";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";

            default -> throw new IllegalArgumentException(
                    "Unsupported content type: " + contentType
            );
        };
    }

    private void deleteDirectory(Path directory) {
        if (directory == null) {
            return;
        }

        try (Stream<Path> files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);

                        } catch (IOException exception) {
                            log.warn(
                                    "Could not delete temporary file {}",
                                    path,
                                    exception
                            );
                        }
                    });

        } catch (IOException exception) {
            log.warn(
                    "Could not clean OCR temp directory {}",
                    directory,
                    exception
            );
        }
    }
}