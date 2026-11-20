package at.fhtw.swen.paperless.genai.service.impl;

import at.fhtw.swen.paperless.genai.service.GenaiClient;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(
        name = "genai.provider",
        havingValue = "gemini"
)
public class GeminiClientImpl implements GenaiClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 2_000L;

    private final Client client;
    private final String model;

    public GeminiClientImpl(
            @Value("${genai.gemini.api-key}") String apiKey,
            @Value("${genai.gemini.model}") String model
    ) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();

        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                log.debug(
                        "Sending Gemini generation request (attempt {}/{})",
                        attempt,
                        MAX_ATTEMPTS
                );

                return generateOnce(prompt);

            } catch (Exception exception) {
                lastException = exception;

                log.warn(
                        "Gemini generation attempt {}/{} failed: {}",
                        attempt,
                        MAX_ATTEMPTS,
                        exception.getMessage()
                );

                if (attempt == MAX_ATTEMPTS) {
                    break;
                }

                waitBeforeRetry(attempt);
            }
        }

        throw new IllegalStateException(
                "Gemini generation failed after "
                        + MAX_ATTEMPTS
                        + " attempts",
                lastException
        );
    }

    private String generateOnce(String prompt) {
        GenerateContentResponse response =
                client.models.generateContent(
                        model,
                        prompt,
                        null
                );

        if (response == null || response.text() == null) {
            throw new IllegalStateException(
                    "Gemini returned an empty response"
            );
        }

        return response.text().trim();
    }

    private void waitBeforeRetry(int attempt) {
        long backoffMillis = INITIAL_BACKOFF_MILLIS
                * (1L << (attempt - 1));

        log.info(
                "Retrying Gemini request in {} ms",
                backoffMillis
        );

        try {
            Thread.sleep(backoffMillis);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting to retry Gemini request",
                    exception
            );
        }
    }
}