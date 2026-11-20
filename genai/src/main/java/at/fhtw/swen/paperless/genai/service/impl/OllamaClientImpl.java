package at.fhtw.swen.paperless.genai.service.impl;

import at.fhtw.swen.paperless.genai.service.GenaiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@ConditionalOnProperty(
        name = "genai.provider",
        havingValue = "ollama"
)
public class OllamaClientImpl implements GenaiClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 2_000L;

    private final RestClient client;
    private final String model;

    public OllamaClientImpl(
            @Value("${genai.ollama.base-url}") String baseUrl,
            @Value("${genai.ollama.model}") String model
    ) {
        this.client = RestClient
                .builder()
                .baseUrl(baseUrl)
                .build();

        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        OllamaRequest request = new OllamaRequest(
                model,
                prompt,
                false
        );

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                log.debug(
                        "Sending Ollama generation request (attempt {}/{})",
                        attempt,
                        MAX_ATTEMPTS
                );

                return generateOnce(request);

            } catch (Exception exception) {
                lastException = exception;

                log.warn(
                        "Ollama generation attempt {}/{} failed: {}",
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
                "Ollama generation failed after "
                        + MAX_ATTEMPTS
                        + " attempts",
                lastException
        );
    }

    private String generateOnce(OllamaRequest request) {
        OllamaResponse response = client
                .post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null || response.response() == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return response.response().trim();
    }

    private void waitBeforeRetry(int attempt) {
        long backoffMillis = INITIAL_BACKOFF_MILLIS
                * (1L << (attempt - 1));

        log.info(
                "Retrying Ollama request in {} ms",
                backoffMillis
        );

        try {
            Thread.sleep(backoffMillis);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting to retry Ollama request",
                    exception
            );
        }
    }

    private record OllamaRequest(
            String model,
            String prompt,
            boolean stream
    ) {
    }

    private record OllamaResponse(
            String model,
            String response,
            boolean done
    ) {
    }
}