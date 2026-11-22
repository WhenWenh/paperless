package worker

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"regexp"
	"strings"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

var uuidPattern = regexp.MustCompile(`(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$`)

func DecodeMessage(body []byte) (DocumentUploadedMessage, error) {
	var message DocumentUploadedMessage
	if err := json.Unmarshal(body, &message); err != nil {
		return message, errors.New("invalid OCR message JSON")
	}
	if !uuidPattern.MatchString(message.DocumentID) || !uuidPattern.MatchString(message.StorageUUID) {
		return message, errors.New("documentId and storageUuid must be UUIDs")
	}
	for _, id := range []*string{message.OwnerID, message.TagID} {
		if id != nil {
			if !uuidPattern.MatchString(*id) {
				return message, errors.New("ownerId and tagId must be UUIDs when present")
			}
			*id = strings.ToLower(*id)
		}
	}
	message.DocumentID = strings.ToLower(message.DocumentID)
	message.StorageUUID = strings.ToLower(message.StorageUUID)
	return message, nil
}

type processor struct {
	openDocument func(context.Context, string) (io.ReadCloser, error)
	publish      func(context.Context, OcrCompletedMessage) error
}

func (p processor) processDocument(ctx context.Context, message DocumentUploadedMessage) error {
	document, err := p.openDocument(ctx, message.StorageUUID)
	if err != nil {
		return fmt.Errorf("load document: %w", err)
	}
	defer document.Close()

	slog.Info("Loaded document for OCR", "documentId", message.DocumentID, "storageUuid", message.StorageUUID)
	text, err := extractText(ctx, document, message.ContentType)
	if err != nil {
		return err
	}

	result := OcrCompletedMessage{
		DocumentID:       message.DocumentID,
		StorageUUID:      message.StorageUUID,
		OwnerID:          message.OwnerID,
		Title:            message.Title,
		OriginalFilename: message.Filename,
		TagID:            message.TagID,
		ContentType:      message.ContentType,
		Text:             text,
	}
	if err := p.publish(ctx, result); err != nil {
		return fmt.Errorf("%w: %w", errPublishResult, err)
	}
	slog.Info("Published OCR result", "documentId", message.DocumentID, "storageUuid", message.StorageUUID, "bytes", len(text))
	return nil
}

func HandleDelivery(ctx context.Context, delivery amqp.Delivery, process func(context.Context, DocumentUploadedMessage) error) error {
	if err := ctx.Err(); err != nil {
		return err
	}

	message, err := DecodeMessage(delivery.Body)
	if err != nil {
		slog.Warn("Rejecting malformed OCR job", "error", err)
		if err := delivery.Nack(false, false); err != nil {
			return fmt.Errorf("reject malformed OCR job: %w", err)
		}
		return nil
	}

	log := slog.With("documentId", message.DocumentID, "storageUuid", message.StorageUUID, "contentType", message.ContentType)
	log.Info("Received OCR job", "redelivered", delivery.Redelivered)
	if extensionFor(message.ContentType) == "" {
		log.Warn("Skipping unsupported document")
	} else if err := process(ctx, message); err != nil {
		log.Error("OCR job failed", "error", err)
		if errors.Is(err, errPublishResult) {
			// Closing the connection requeues the unacknowledged job and discards any late publisher confirm.
			return err
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(2 * time.Second):
		}
		if err := delivery.Nack(false, true); err != nil {
			return fmt.Errorf("request OCR job redelivery: %w", err)
		}
		return nil
	}

	if err := ctx.Err(); err != nil {
		return err
	}
	if err := delivery.Ack(false); err != nil {
		return fmt.Errorf("acknowledge OCR job: %w", err)
	}
	return nil
}
