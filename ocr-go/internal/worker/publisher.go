package worker

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

var errPublishResult = errors.New("publish OCR result")

func newResultPublisher(channel rabbitChannel) (func(context.Context, OcrCompletedMessage) error, error) {
	_, err := channel.QueueDeclare(ocrCompletedQueue, true, false, false, false, amqp.Table{
		"x-dead-letter-exchange":    "",
		"x-dead-letter-routing-key": ocrCompletedDeadLetterQueue,
	})
	if err != nil {
		return nil, fmt.Errorf("declare OCR result queue: %w", err)
	}
	if err := channel.Confirm(false); err != nil {
		return nil, fmt.Errorf("enable OCR result publisher confirms: %w", err)
	}

	// The worker processes one delivery at a time, so only one publish can be in flight.
	returns := channel.NotifyReturn(make(chan amqp.Return, 1))
	confirms := channel.NotifyPublish(make(chan amqp.Confirmation, 1))
	return func(ctx context.Context, result OcrCompletedMessage) error {
		body, err := json.Marshal(result)
		if err != nil {
			return err
		}
		ctx, cancel := context.WithTimeout(ctx, 15*time.Second)
		defer cancel()

		err = channel.PublishWithContext(ctx, "", ocrCompletedQueue, true, false, amqp.Publishing{
			ContentType:  "application/json",
			DeliveryMode: amqp.Persistent,
			Body:         body,
		})
		if err != nil {
			return err
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case confirmation, open := <-confirms:
			if !open || !confirmation.Ack {
				return errors.New("RabbitMQ did not confirm the OCR result")
			}
		}

		// RabbitMQ returns an unroutable mandatory publish before confirming it.
		select {
		case returned, open := <-returns:
			if !open {
				return errors.New("OCR result publisher channel closed")
			}
			return fmt.Errorf("OCR result unroutable: %s", returned.ReplyText)
		default:
			return nil
		}
	}, nil
}
