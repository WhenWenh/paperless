package worker

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestProviderRequests(t *testing.T) {
	prompt, err := documentPrompt(ocrCompletedMessage{Text: new("OCR text")})
	if err != nil {
		t.Fatal(err)
	}
	t.Run("Ollama", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			var body struct {
				Model, System, Prompt string
				Stream                *bool
			}
			if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
				t.Error(err)
			}
			if r.Method != http.MethodPost || r.URL.Path != "/api/generate" ||
				body.Model != "qwen2.5:latest" || body.System != summaryInstructions || body.Prompt != prompt ||
				body.Stream == nil || *body.Stream {
				t.Errorf("unexpected Ollama request: %s %s %+v", r.Method, r.URL.Path, body)
			}
			_, _ = io.WriteString(w, `{"response":"  A concise summary.  ","done":true}`)
		}))
		defer server.Close()
		generate := ollamaGenerator(server.Client(), server.URL, "qwen2.5:latest")
		result, err := generate(t.Context(), prompt)
		if err != nil || result != "A concise summary." {
			t.Fatalf("result = %q, error = %v", result, err)
		}
	})

	t.Run("Gemini", func(t *testing.T) {
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			var body struct {
				SystemInstruction geminiContent
				Contents          []geminiContent
			}
			if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
				t.Error(err)
			}
			if r.Method != http.MethodPost || r.URL.Path != "/v1beta/models/gemini-3.5-flash-lite:generateContent" ||
				r.Header.Get("x-goog-api-key") != "test-key" || r.URL.RawQuery != "" ||
				len(body.SystemInstruction.Parts) != 1 || body.SystemInstruction.Parts[0].Text != summaryInstructions ||
				len(body.Contents) != 1 || body.Contents[0].Role != "user" ||
				len(body.Contents[0].Parts) != 1 || body.Contents[0].Parts[0].Text != prompt {
				t.Errorf("unexpected Gemini request: %s %s %+v", r.Method, r.URL.Path, body)
			}
			_, _ = io.WriteString(w, `{"candidates":[{"content":{"parts":[{"text":"Private thinking","thought":true},{"text":" A concise "},{"text":"summary. "}]}}]}`)
		}))
		defer server.Close()
		generate := geminiGenerator(server.Client(), server.URL, "gemini-3.5-flash-lite", "test-key")
		result, err := generate(t.Context(), prompt)
		if err != nil || result != "A concise summary." {
			t.Fatalf("result = %q, error = %v", result, err)
		}
	})
}

func TestGenerationRetriesTransientErrors(t *testing.T) {
	for _, test := range []struct {
		status   int
		attempts int
	}{
		{http.StatusServiceUnavailable, 3},
		{http.StatusTooManyRequests, 3},
		{http.StatusUnauthorized, 1},
		{http.StatusNotFound, 1},
	} {
		attempts := 0
		generate := func(context.Context, string) (string, error) {
			attempts++
			return "", &generationStatusError{status: test.status}
		}
		if _, err := generateWithRetry(t.Context(), generate, "prompt", 0); err == nil || attempts != test.attempts {
			t.Errorf("HTTP %d: attempts = %d, want %d, error = %v", test.status, attempts, test.attempts, err)
		}
	}
}

func TestProviderConfig(t *testing.T) {
	t.Setenv("RABBITMQ_USER", "test")
	t.Setenv("RABBITMQ_PASSWORD", "test")
	t.Setenv("GENAI_PROVIDER", "")
	t.Setenv("GOOGLE_API_KEY", "")
	t.Setenv("ELASTICSEARCH_URL", "")
	t.Setenv("OLLAMA_BASE_URL", "")
	config, err := LoadConfig()
	if err != nil || config.Provider != "ollama" {
		t.Fatalf("expected Ollama default, got %q, error = %v", config.Provider, err)
	}

	t.Setenv("GENAI_PROVIDER", "gemini")
	if _, err := LoadConfig(); err == nil {
		t.Fatal("Gemini accepted without an API key")
	}
	t.Setenv("GOOGLE_API_KEY", "test-key")
	t.Setenv("GEMINI_MODEL", "models/gemini-3.5-flash-lite")
	config, err = LoadConfig()
	if err != nil || config.Provider != "gemini" {
		t.Fatalf("expected Gemini, got %q, error = %v", config.Provider, err)
	}
}
