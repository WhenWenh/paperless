package worker

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

func declareQueues(channel *amqp.Channel) error {
	for _, name := range []string{ocrCompletedDeadLetterQueue, summaryGeneratedQueue} {
		if _, err := channel.QueueDeclare(name, true, false, false, false, nil); err != nil {
			return fmt.Errorf("declare %s: %w", name, err)
		}
	}
	_, err := channel.QueueDeclare(ocrCompletedQueue, true, false, false, false, amqp.Table{
		"x-dead-letter-exchange":    "",
		"x-dead-letter-routing-key": ocrCompletedDeadLetterQueue,
	})
	if err != nil {
		return fmt.Errorf("declare %s: %w", ocrCompletedQueue, err)
	}
	return nil
}

func newSummaryPublisher(channel *amqp.Channel) (func(context.Context, summaryGeneratedMessage) error, error) {
	if err := channel.Confirm(false); err != nil {
		return nil, fmt.Errorf("enable summary publisher confirms: %w", err)
	}

	// Only one delivery is processed at a time, so one publish can be in flight.
	returns := channel.NotifyReturn(make(chan amqp.Return, 1))
	confirms := channel.NotifyPublish(make(chan amqp.Confirmation, 1))
	return func(ctx context.Context, result summaryGeneratedMessage) error {
		body, err := json.Marshal(result)
		if err != nil {
			return err
		}
		ctx, cancel := context.WithTimeout(ctx, 15*time.Second)
		defer cancel()

		err = channel.PublishWithContext(ctx, "", summaryGeneratedQueue, true, false, amqp.Publishing{
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
				return errors.New("RabbitMQ did not confirm the summary")
			}
		}
		// RabbitMQ returns an unroutable mandatory publish before confirming it.
		select {
		case returned, open := <-returns:
			if !open {
				return errors.New("summary publisher channel closed")
			}
			return fmt.Errorf("summary unroutable: %s", returned.ReplyText)
		default:
			return nil
		}
	}, nil
}
