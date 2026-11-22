package worker

import (
	"errors"
	"fmt"
	"net"
	"net/url"
	"os"
	"strings"
)

type Config struct {
	RabbitURL        string
	ElasticsearchURL string
	Provider         string
	OllamaURL        string
	OllamaModel      string
	GeminiAPIKey     string
	GeminiModel      string
}

func LoadConfig() (Config, error) {
	user, password := os.Getenv("RABBITMQ_USER"), os.Getenv("RABBITMQ_PASSWORD")
	if user == "" || password == "" {
		return Config{}, errors.New("RABBITMQ_USER and RABBITMQ_PASSWORD are required")
	}
	rabbitURL := url.URL{
		Scheme: "amqp",
		User:   url.UserPassword(user, password),
		Host:   net.JoinHostPort(envOrDefault("RABBITMQ_HOST", "localhost"), envOrDefault("RABBITMQ_PORT", "5672")),
		Path:   "/",
	}
	config := Config{
		RabbitURL:        rabbitURL.String(),
		ElasticsearchURL: envOrDefault("ELASTICSEARCH_URL", "http://localhost:9200"),
		Provider:         strings.ToLower(envOrDefault("GENAI_PROVIDER", "ollama")),
		OllamaURL:        envOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
		OllamaModel:      envOrDefault("OLLAMA_MODEL", "qwen2.5:latest"),
		GeminiAPIKey:     strings.TrimSpace(os.Getenv("GOOGLE_API_KEY")),
		GeminiModel:      envOrDefault("GEMINI_MODEL", "gemini-3.5-flash-lite"),
	}
	if err := validateHTTPURL("ELASTICSEARCH_URL", config.ElasticsearchURL); err != nil {
		return Config{}, err
	}
	switch config.Provider {
	case "ollama":
		if err := validateHTTPURL("OLLAMA_BASE_URL", config.OllamaURL); err != nil {
			return Config{}, err
		}
	case "gemini":
		if config.GeminiAPIKey == "" {
			return Config{}, errors.New("GOOGLE_API_KEY is required when GENAI_PROVIDER=gemini")
		}
		if !geminiModelPattern.MatchString(strings.TrimPrefix(config.GeminiModel, "models/")) {
			return Config{}, errors.New("GEMINI_MODEL must be a Gemini model ID")
		}
	default:
		return Config{}, errors.New("GENAI_PROVIDER must be ollama or gemini")
	}
	return config, nil
}

func validateHTTPURL(name, value string) error {
	address, err := url.Parse(value)
	if err != nil || address.Hostname() == "" || (address.Scheme != "http" && address.Scheme != "https") ||
		address.User != nil || address.RawQuery != "" || address.ForceQuery || address.Fragment != "" {
		return fmt.Errorf("%s must be an HTTP or HTTPS base URL", name)
	}
	return nil
}

func envOrDefault(name, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(name)); value != "" {
		return value
	}
	return fallback
}
