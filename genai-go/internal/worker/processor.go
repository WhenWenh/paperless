package worker

import (
	"context"
	"fmt"
	"log/slog"
	"strings"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

type processor struct {
	index      func(context.Context, ocrCompletedMessage) error
	generate   generator
	publish    func(context.Context, summaryGeneratedMessage) error
	indexDelay time.Duration
}

func (p processor) handle(ctx context.Context, delivery amqp.Delivery) error {
	if err := ctx.Err(); err != nil {
		return err
	}
	message, err := decodeOcrResult(delivery.Body)
	if err != nil {
		slog.Warn("Dead-lettering malformed OCR result", "error", err)
		return nack(delivery, false)
	}
	log := slog.With("documentId", message.DocumentID, "storageUuid", valueOrEmpty(message.StorageUUID))
	if err := p.index(ctx, message); err != nil {
		log.Error("Indexing failed; retrying OCR result", "error", err)
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(p.indexDelay):
			return nack(delivery, true)
		}
	}
	log.Info("Successfully indexed document")

	if strings.TrimSpace(valueOrEmpty(message.Text)) == "" {
		log.Info("Skipping summary because OCR text is empty")
		return ack(ctx, delivery)
	}
	prompt, err := documentPrompt(message)
	if err != nil {
		return fmt.Errorf("build summary prompt: %w", err)
	}
	summary, err := p.generate(ctx, prompt)
	if ctx.Err() != nil {
		return ctx.Err()
	}
	if err != nil {
		log.Error("Summary generation failed; dead-lettering OCR result", "error", err)
		return nack(delivery, false)
	}
	log.Info("Successfully generated summary")

	if err := p.publish(ctx, summaryGeneratedMessage{DocumentID: message.DocumentID, Summary: summary}); err != nil {
		// Closing the connection requeues the unacknowledged OCR result.
		return fmt.Errorf("publish generated summary for document %s: %w", message.DocumentID, err)
	}
	log.Info("Published generated summary")
	return ack(ctx, delivery)
}

func ack(ctx context.Context, delivery amqp.Delivery) error {
	if err := ctx.Err(); err != nil {
		return err
	}
	if err := delivery.Ack(false); err != nil {
		return fmt.Errorf("acknowledge OCR result: %w", err)
	}
	return nil
}

func nack(delivery amqp.Delivery, requeue bool) error {
	if err := delivery.Nack(false, requeue); err != nil {
		return fmt.Errorf("reject OCR result: %w", err)
	}
	return nil
}
