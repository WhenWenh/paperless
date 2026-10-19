package worker

import (
	"context"       // Ermöglicht Abbruch beim Herunterfahren.
	"encoding/json" // Entspricht hier dem JSON-Konverter.
	"errors"        // Einfache Fehler erzeugen.
	"fmt"           // Fehler mit zusätzlichem Kontext umschließen.
	"log/slog"      // Logging.
	"time"          // Pause vor erneuter Zustellung.

	amqp "github.com/rabbitmq/amqp091-go"
)

func DecodeMessage(body []byte) (DocumentUploadedMessage, error) {
	var message DocumentUploadedMessage

	// &message übergibt die Adresse der Struktur.
	// Unmarshal kann dadurch deren Felder befüllen.
	if err := json.Unmarshal(body, &message); err != nil {
		// Keine vollständige Nachricht im Fehlertext ausgeben.
		return message, errors.New("invalid OCR message JSON")
	}

	if message.DocumentID == "" || message.StorageUUID == "" {
		return message, errors.New(
			"documentId and storageUuid are required",
		)
	}

	if message.ContentType != "application/pdf" {
		return message, errors.New("expected application/pdf")
	}

	return message, nil
}

func processDocument(message DocumentUploadedMessage) error {
	slog.Info(
		"Start processing OCR job",
		"documentTitle", message.Title,
		"documentId", message.DocumentID,
		"documentName", message.Filename,
		"storageUuid", message.StorageUUID,
		"contentType", message.ContentType,
	)

	// TODO: PDF laden und OCR-Engine aufrufen.

	return nil
}

func HandleDelivery(
	ctx context.Context,
	delivery amqp.Delivery,
) error {
	message, processingErr := DecodeMessage(delivery.Body)

	if processingErr == nil {
		slog.Info(
			"OCR job received",
			"documentId", message.DocumentID,
			"storageUuid", message.StorageUUID,
			"redelivered", delivery.Redelivered,
		)

		// Ergebnis der Verarbeitung merken.
		processingErr = processDocument(message)
	}

	if processingErr != nil {
		slog.Error(
			"OCR job failed; returning message to queue",
			"documentId", message.DocumentID,
			"storageUuid", message.StorageUUID,
			"error", processingErr,
		)

		select {
		case <-ctx.Done():
			return ctx.Err()

		case <-time.After(2 * time.Second):
			// Nach der Pause weiter zur Rückgabe der Nachricht.
		}

		// false: nur diese Nachricht.
		// true: erneut in die Queue stellen.
		if err := delivery.Nack(false, true); err != nil {
			return fmt.Errorf("request redelivery: %w", err)
		}

		// Der Verarbeitungsfehler wurde behandelt.
		// Das bedeutet NICHT, dass die OCR erfolgreich war.
		return nil
	}

	// Verarbeitung erfolgreich: einzelne Nachricht bestätigen.
	if err := delivery.Ack(false); err != nil {
		return fmt.Errorf("acknowledge OCR job: %w", err)
	}

	slog.Info(
		"OCR placeholder completed; acknowledgement sent",
		"documentId", message.DocumentID,
		"storageUuid", message.StorageUUID,
	)

	return nil
}
