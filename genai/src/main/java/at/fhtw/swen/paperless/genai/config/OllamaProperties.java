package at.fhtw.swen.paperless.genai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "genai.ollama")
public record OllamaProperties(
        String baseUrl,
        String model
) {
}