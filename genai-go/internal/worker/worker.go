package worker

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"strings"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

func Run(ctx context.Context, config Config) error {
	indexer := documentIndexer{
		client:   &http.Client{Timeout: 10 * time.Second},
		indexURL: strings.TrimRight(config.ElasticsearchURL, "/") + "/documents",
	}
	if err := indexer.ensureIndex(ctx); err != nil {
		return err
	}
	generate := newGenerator(config)

	connection, err := amqp.Dial(config.RabbitURL)
	if err != nil {
		return errors.New("RabbitMQ connection failed; check broker and configuration")
	}
	defer connection.Close()

	consumer, err := connection.Channel()
	if err != nil {
		return fmt.Errorf("open RabbitMQ consumer channel: %w", err)
	}
	defer consumer.Close()
	if err := declareQueues(consumer); err != nil {
		return err
	}
	if err := consumer.Qos(1, 0, false); err != nil {
		return fmt.Errorf("configure GenAI prefetch: %w", err)
	}

	publisher, err := connection.Channel()
	if err != nil {
		return fmt.Errorf("open RabbitMQ publisher channel: %w", err)
	}
	defer publisher.Close()
	publisherClosed := publisher.NotifyClose(make(chan *amqp.Error, 1))
	publish, err := newSummaryPublisher(publisher)
	if err != nil {
		return err
	}
	deliveryProcessor := processor{
		index: indexer.index,
		generate: func(ctx context.Context, prompt string) (string, error) {
			return generateWithRetry(ctx, generate, prompt, 2*time.Second)
		},
		publish:    publish,
		indexDelay: 2 * time.Second,
	}

	deliveries, err := consumer.Consume(ocrCompletedQueue, "", false, false, false, false, nil)
	if err != nil {
		return fmt.Errorf("register GenAI consumer: %w", err)
	}
	healthServer, serverErrors, err := startHealth(func(ctx context.Context) bool {
		return !connection.IsClosed() && !consumer.IsClosed() && !publisher.IsClosed() && indexer.ready(ctx)
	})
	if err != nil {
		return fmt.Errorf("start GenAI health server: %w", err)
	}
	defer healthServer.Close()
	slog.Info("GenAI worker ready", "provider", config.Provider, "queue", ocrCompletedQueue)
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		case err := <-serverErrors:
			return fmt.Errorf("GenAI health server stopped: %w", err)
		case <-publisherClosed:
			return errors.New("GenAI publisher channel closed")
		case delivery, open := <-deliveries:
			if !open {
				return errors.New("GenAI delivery stream closed")
			}
			if err := deliveryProcessor.handle(ctx, delivery); err != nil {
				return err
			}
		}
	}
}

func startHealth(ready func(context.Context) bool) (*http.Server, <-chan error, error) {
	listener, err := net.Listen("tcp", ":8083")
	if err != nil {
		return nil, nil, err
	}
	mux := http.NewServeMux()
	health := func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()
		if !ready(ctx) {
			http.Error(w, "RabbitMQ or Elasticsearch unavailable", http.StatusServiceUnavailable)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"status":"UP"}`))
	}
	mux.HandleFunc("GET /health", health)
	mux.HandleFunc("GET /actuator/health", health)
	server := &http.Server{Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	serverErrors := make(chan error, 1)
	go func() {
		serverErrors <- server.Serve(listener)
	}()
	return server, serverErrors, nil
}
