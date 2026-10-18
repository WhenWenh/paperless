package main

import (
	"context"  // Stoppsignal.
	"errors"   // Einfache Fehlerwerte.
	"fmt"      // Fehler mit Kontext.
	"log/slog" // Logging.
	"net/http"

	amqp "github.com/rabbitmq/amqp091-go"
)

func runWorker(ctx context.Context, config Config) error {
	return runWorkerWith(ctx, config, dialRabbit, startHealthServer)
}

// Dependencies are passed explicitly so tests do not consume real upload jobs.
type rabbitChannel interface {
	QueueDeclare(string, bool, bool, bool, bool, amqp.Table) (amqp.Queue, error)
	Qos(int, int, bool) error
	Consume(string, string, bool, bool, bool, bool, amqp.Table) (<-chan amqp.Delivery, error)
	Close() error
	IsClosed() bool
}

type rabbitConnection interface {
	Channel() (rabbitChannel, error)
	Close() error
	IsClosed() bool
}

type liveConnection struct { *amqp.Connection }

func (c liveConnection) Channel() (rabbitChannel, error) { return c.Connection.Channel() }

func dialRabbit(address string) (rabbitConnection, error) {
	c, err := amqp.Dial(address)
	if err != nil { return nil, err }
	return liveConnection{c}, nil
}

func runWorkerWith(ctx context.Context, config Config,
	dial func(string) (rabbitConnection, error),
	startHealth func(string, func() bool) (*http.Server, <-chan error, error),
) error {
	// Verbindung zu RabbitMQ herstellen.
	connection, err := dial(config.rabbitURL())
	if err != nil {
		return errors.New(
			"RabbitMQ connection failed; check broker and configuration",
		)
	}

	// Verbindung beim Verlassen dieser Funktion schließen.
	defer connection.Close()

	// AMQP-Kanal innerhalb der Verbindung öffnen.
	channel, err := connection.Channel()
	if err != nil {
		return fmt.Errorf("open RabbitMQ channel: %w", err)
	}
	defer channel.Close()

	// Entspricht new Queue(QueueNames.DOCUMENT_UPLOAD_QUEUE, true).
	queue, err := channel.QueueDeclare(
		documentUploadQueue, // "document.uploaded"
		true,                // dauerhaft
		false,               // nicht automatisch löschen
		false,               // nicht exklusiv
		false,               // Bestätigung der Deklaration abwarten
		nil,                 // keine zusätzlichen Queue-Argumente
	)
	if err != nil {
		return fmt.Errorf("declare upload queue: %w", err)
	}

	// Maximal eine unbestätigte Nachricht für diesen Consumer.
	if err := channel.Qos(1, 0, false); err != nil {
		return fmt.Errorf("configure prefetch: %w", err)
	}

	// Consumer registrieren.
	deliveries, err := channel.Consume(
		queue.Name, // Queue abonnieren.
		"",         // RabbitMQ erzeugt einen Consumer-Namen.
		false,      // Nicht sofort automatisch bestätigen.
		false,      // Andere Consumer wären grundsätzlich erlaubt.
		false,      // no-local, für RabbitMQ hier false.
		false,      // Registrierung bestätigen lassen.
		nil,        // Keine zusätzlichen Consumer-Argumente.
	)
	if err != nil {
		return fmt.Errorf("register consumer: %w", err)
	}

	// Eigenen Healthcheck starten.
	server, serverErrors, err := startHealth(
		config.HealthAddress,

		// Funktion wird bei jedem Healthcheck aufgerufen.
		func() bool {
			return !connection.IsClosed() && !channel.IsClosed()
		},
	)
	if err != nil {
		return fmt.Errorf("start health server: %w", err)
	}

	// HTTP-Server beim Beenden schließen.
	defer server.Close()

	slog.Info(
		"OCR worker ready",
		"queue", queue.Name,
	)

	// Ohne Bedingung ist for eine Endlosschleife.
	for {
		// Auf eines der folgenden Ereignisse warten.
		select {
		case <-ctx.Done():
			// Ctrl+C oder Docker-Stopp.
			slog.Info("OCR worker shutting down")
			return nil

		case err := <-serverErrors:
			// Health-Server unerwartet beendet.
			return fmt.Errorf("health server stopped: %w", err)

		case delivery, open := <-deliveries:
			// open=false bedeutet: Zustellungsstrom geschlossen.
			if !open {
				return errors.New(
					"RabbitMQ delivery stream closed",
				)
			}

			// Entspricht dem Aufruf deiner receive(...)-Methode.
			if err := handleDelivery(ctx, delivery); err != nil {
				// Angeforderter Stopp ist kein Betriebsfehler.
				if ctx.Err() != nil {
					return nil
				}

				// Technischer Fehler, beispielsweise ACK fehlgeschlagen.
				return err
			}
		}
	}
}
