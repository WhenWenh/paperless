package worker

import (
	"context"
	"errors"
	"fmt"
	"reflect"
	"testing"

	amqp "github.com/rabbitmq/amqp091-go"
)

const testDocumentID = "10000000-0000-4000-8000-000000000001"

type deliveryRecorder struct {
	events *[]string
}

func (r deliveryRecorder) Ack(_ uint64, multiple bool) error {
	if multiple {
		return errors.New("unexpected multiple ACK")
	}
	*r.events = append(*r.events, "ack")
	return nil
}

func (r deliveryRecorder) Nack(_ uint64, multiple, requeue bool) error {
	if multiple {
		return errors.New("unexpected multiple NACK")
	}
	event := "dead-letter"
	if requeue {
		event = "requeue"
	}
	*r.events = append(*r.events, event)
	return nil
}

func (r deliveryRecorder) Reject(tag uint64, requeue bool) error {
	return r.Nack(tag, false, requeue)
}

func TestProcessOCRResult(t *testing.T) {
	failure := errors.New("service unavailable")
	for _, test := range []struct {
		name                  string
		text                  string
		invalid               bool
		index, model, publish error
		want                  []string
	}{
		{name: "success", text: "example", want: []string{"index", "generate", "publish", "ack"}},
		{name: "blank text", text: " \n", want: []string{"index", "ack"}},
		{name: "invalid JSON", invalid: true, want: []string{"dead-letter"}},
		{name: "Elasticsearch unavailable", text: "example", index: failure, want: []string{"index", "requeue"}},
		{name: "provider unavailable", text: "example", model: failure, want: []string{"index", "generate", "dead-letter"}},
		{name: "publish failed", text: "example", publish: failure, want: []string{"index", "generate", "publish"}},
	} {
		t.Run(test.name, func(t *testing.T) {
			var events []string
			body := fmt.Sprintf(`{"documentId":%q,"tagId":null,"text":%q}`, testDocumentID, test.text)
			if test.invalid {
				body = "{"
			}
			delivery := amqp.Delivery{Body: []byte(body), Acknowledger: deliveryRecorder{events: &events}}
			processor := processor{
				index: func(_ context.Context, message ocrCompletedMessage) error {
					events = append(events, "index")
					if message.DocumentID != testDocumentID || message.TagID != nil {
						t.Errorf("unexpected OCR message: %+v", message)
					}
					return test.index
				},
				generate: func(_ context.Context, prompt string) (string, error) {
					events = append(events, "generate")
					if prompt != `{"title":"","text":"example"}` {
						t.Errorf("unexpected prompt: %s", prompt)
					}
					return "Summary", test.model
				},
				publish: func(_ context.Context, message summaryGeneratedMessage) error {
					events = append(events, "publish")
					if message.DocumentID != testDocumentID || message.Summary != "Summary" {
						t.Errorf("unexpected summary message: %+v", message)
					}
					return test.publish
				},
			}
			if err := processor.handle(t.Context(), delivery); !errors.Is(err, test.publish) {
				t.Fatalf("error = %v, want %v", err, test.publish)
			}
			if !reflect.DeepEqual(events, test.want) {
				t.Fatalf("events = %v, want %v", events, test.want)
			}
		})
	}
}
