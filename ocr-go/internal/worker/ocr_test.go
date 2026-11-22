package worker

import (
	"context"
	"errors"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"

	amqp "github.com/rabbitmq/amqp091-go"
)

func fakeOCRTools(t *testing.T, poppler, tesseract string) {
	t.Helper()
	if _, err := exec.LookPath("sh"); err != nil {
		t.Skip("POSIX shell needed for OCR command fixtures")
	}
	directory := t.TempDir()
	t.Setenv("PATH", directory+string(os.PathListSeparator)+os.Getenv("PATH"))
	for name, script := range map[string]string{"pdftoppm": poppler, "tesseract": tesseract} {
		if err := os.WriteFile(filepath.Join(directory, name), []byte("#!/bin/sh\n"+script+"\n"), 0700); err != nil {
			t.Fatal(err)
		}
	}
}

func TestExtractPDFOrdersPagesAndCleansWorkspace(t *testing.T) {
	workspace := t.TempDir()
	t.Setenv("TMPDIR", workspace)
	fakeOCRTools(t, `
test "$1" = "-png" && test -s "$2" || exit 2
touch "$3-10.png" "$3-2.png" "$3-1.png"
`, `
test "$2" = "stdout" || exit 2
printf '%s' "${1##*/}"
`)
	text, err := extractText(context.Background(), strings.NewReader("PDF input"), "application/pdf")
	if err != nil || text != "page-1.png\npage-2.png\npage-10.png\n" {
		t.Fatalf("text = %q, error = %v", text, err)
	}
	files, err := os.ReadDir(workspace)
	if err != nil || len(files) != 0 {
		t.Fatalf("workspace still contains %v, error = %v", files, err)
	}
}

func TestExtractPDFWithNoPagesFails(t *testing.T) {
	fakeOCRTools(t, "exit 0", "exit 2")
	if _, err := extractText(context.Background(), strings.NewReader("PDF input"), "application/pdf"); err == nil {
		t.Fatal("accepted a conversion without any pages")
	}
}

func TestProcessorPublishesEmptyImageResultWithMetadata(t *testing.T) {
	fakeOCRTools(t, "exit 2", `
test "${1##*/}" = "input.webp" && test "$2" = "stdout" && test -s "$1"
`)
	message, err := DecodeMessage([]byte(`{
		"documentId":"217df5bc-661c-4c0b-843f-93bdd1273e94",
		"storageUuid":"1E195006-626F-4437-B913-9B8358607EF9",
		"ownerId":"38b97550-66a3-4dab-a4c5-c117b37e1a18",
		"tagId":null,
		"title":"Blank document",
		"filename":"blank.webp",
		"contentType":"IMAGE/WEBP"
	}`))
	if err != nil {
		t.Fatal(err)
	}
	p := processor{
		openDocument: func(_ context.Context, key string) (io.ReadCloser, error) {
			if key != "1e195006-626f-4437-b913-9b8358607ef9" {
				t.Fatalf("unexpected storage key %q", key)
			}
			return io.NopCloser(strings.NewReader("image")), nil
		},
		publish: func(_ context.Context, result OcrCompletedMessage) error {
			if result.DocumentID != message.DocumentID || result.StorageUUID != message.StorageUUID ||
				result.OwnerID == nil || *result.OwnerID != *message.OwnerID || result.TagID != nil ||
				result.Title != message.Title || result.OriginalFilename != message.Filename ||
				result.ContentType != message.ContentType || result.Text != "" {
				t.Fatalf("unexpected OCR result: %+v", result)
			}
			return nil
		},
	}
	if err := p.processDocument(context.Background(), message); err != nil {
		t.Fatal(err)
	}
}

func TestPublishFailureLeavesUploadUnacknowledged(t *testing.T) {
	fakeOCRTools(t, "exit 2", "printf 'text'")
	p := processor{
		openDocument: func(context.Context, string) (io.ReadCloser, error) {
			return io.NopCloser(strings.NewReader("image")), nil
		},
		publish: func(context.Context, OcrCompletedMessage) error {
			return errors.New("publisher unavailable")
		},
	}
	// A nil acknowledger makes an attempted ACK or NACK return an error.
	delivery := amqp.Delivery{Body: []byte(`{
		"documentId":"217df5bc-661c-4c0b-843f-93bdd1273e94",
		"storageUuid":"1e195006-626f-4437-b913-9b8358607ef9",
		"contentType":"image/png"
	}`)}
	if err := HandleDelivery(context.Background(), delivery, p.processDocument); !errors.Is(err, errPublishResult) {
		t.Fatalf("wanted publish error, got %v", err)
	}
}
