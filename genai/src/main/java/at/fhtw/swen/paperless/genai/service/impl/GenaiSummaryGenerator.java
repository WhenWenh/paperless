package at.fhtw.swen.paperless.genai.service.impl;

import at.fhtw.swen.paperless.genai.service.GenaiClient;
import at.fhtw.swen.paperless.genai.service.SummaryGenerator;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import com.hubspot.jinjava.Jinjava;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class GenaiSummaryGenerator implements SummaryGenerator {

    private static final String TEMPLATE_PATH =
            "prompts/summary.j2";

    private final Jinjava jinjava;
    private final GenaiClient genaiClient;
    private final String template;

    public GenaiSummaryGenerator(GenaiClient genaiClient) {
        this.jinjava = new Jinjava();
        this.genaiClient = genaiClient;
        this.template = loadTemplate();
    }

    @Override
    public String generate(OcrCompletedMessage message) {
        if (message.text() == null || message.text().isBlank()) {
            throw new IllegalArgumentException(
                    "Cannot generate a summary without OCR text"
            );
        }

        String title = message.title() == null
                ? ""
                : message.title();

        String prompt = jinjava.render(
                template,
                Map.of(
                        "title", title,
                        "text", message.text()
                )
        );

        return genaiClient.generate(prompt);
    }

    private String loadTemplate() {
        ClassPathResource resource =
                new ClassPathResource(TEMPLATE_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load summary prompt template: "
                            + TEMPLATE_PATH,
                    exception
            );
        }
    }
}