package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"paperless/genai-go/internal/worker"
)

func main() {
	slog.SetDefault(slog.New(slog.NewJSONHandler(os.Stdout, nil)))
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	config, err := worker.LoadConfig()
	if err != nil {
		slog.Error("Invalid GenAI configuration", "error", err)
		os.Exit(1)
	}
	if err := worker.Run(ctx, config); err != nil && ctx.Err() == nil {
		slog.Error("GenAI worker stopped", "error", err)
		os.Exit(1)
	}
}
