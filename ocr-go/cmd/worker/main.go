package main

import (
	"paperless/ocr-go/internal/worker"

	"context"   // Gemeinsames Signal zum Beenden.
	"log/slog"  // Strukturiertes Logging.
	"os"        // Standardausgabe und Exit-Code.
	"os/signal" // Betriebssystemsignale empfangen.
	"syscall"   // Enthält SIGTERM für Docker-Stopp.
)

func main() {
	// Logs als JSON auf die Standardausgabe schreiben.
	// Docker sammelt diese Ausgabe für "docker compose logs".
	slog.SetDefault(
		slog.New(slog.NewJSONHandler(os.Stdout, nil)),
	)

	// Context wird beendet, wenn Ctrl+C oder SIGTERM eintrifft.
	ctx, stop := signal.NotifyContext(
		context.Background(),
		os.Interrupt,
		syscall.SIGTERM,
	)

	// Beim regulären Verlassen der Funktion Signalbehandlung aufräumen.
	defer stop()

	// Konfiguration aus config.go laden.
	config, err := worker.LoadConfig()

	// Go behandelt erwartbare Fehler über Rückgabewerte.
	if err != nil {
		slog.Error("Invalid configuration", "error", err)

		os.Exit(1)
	}

	// Verbindung, Consumer und Healthcheck starten.
	if err := worker.Run(ctx, config); err != nil {
		slog.Error("OCR worker stopped", "error", err)
		os.Exit(1)
	}
}
