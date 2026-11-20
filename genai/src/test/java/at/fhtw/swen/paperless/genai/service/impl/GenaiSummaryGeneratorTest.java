package at.fhtw.swen.paperless.genai.service.impl;

import at.fhtw.swen.paperless.genai.service.GenaiClient;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GenaiSummaryGeneratorTest {

    private final GenaiClient client =
            mock(GenaiClient.class);

    private final GenaiSummaryGenerator generator =
            new GenaiSummaryGenerator(client);

    @Test
    void rendersDocumentIntoPromptAndReturnsClientResponse() {
        OcrCompletedMessage message = message(
                "Invoice",
                "The invoice amount is 120 euros."
        );

        when(client.generate(anyString()))
                .thenReturn("An invoice for 120 euros.");

        String result = generator.generate(message);

        ArgumentCaptor<String> prompt =
                ArgumentCaptor.forClass(String.class);

        verify(client).generate(prompt.capture());

        assertThat(prompt.getValue())
                .contains("Invoice")
                .contains("The invoice amount is 120 euros.")
                .doesNotContain("{{ title }}")
                .doesNotContain("{{ text }}");

        assertThat(result)
                .isEqualTo("An invoice for 120 euros.");
    }

    @Test
    void rejectsBlankTextWithoutCallingClient() {
        assertThatThrownBy(() ->
                generator.generate(message("Invoice", " \n "))
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(client);
    }

    @Test
    void missingTitleDoesNotPreventGeneration() {
        when(client.generate(anyString()))
                .thenReturn("Summary");

        String result = generator.generate(
                message(null, "Document content")
        );

        assertThat(result).isEqualTo("Summary");

        verify(client).generate(anyString());
    }

    private OcrCompletedMessage message(
            String title,
            String text
    ) {
        return new OcrCompletedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                title,
                "test.pdf",
                null,
                "application/pdf",
                text
        );
    }
}