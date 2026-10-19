package worker

import (
	"net"      // TCP-Port öffnen.
	"net/http" // HTTP-Server und Antworten.
	"time"     // Timeout konfigurieren.
)

// ready ist eine übergebene Funktion, die Bereitschaft prüft.
// Rückgabe: Server, Fehlerkanal, möglicher Startfehler.
func StartHealthServer(
	address string,
	ready func() bool,
) (*http.Server, <-chan error, error) {
	// Port sofort öffnen, damit Portkonflikte sofort auffallen.
	listener, err := net.Listen("tcp", address)
	if err != nil {
		return nil, nil, err
	}

	// HTTP-Router anlegen.
	mux := http.NewServeMux()

	// Handler für GET /health registrieren.
	mux.HandleFunc("/health", func(
		writer http.ResponseWriter,
		request *http.Request,
	) {
		// Zustand über die übergebene Funktion prüfen.
		if !ready() {
			http.Error(
				writer,
				"RabbitMQ unavailable",
				http.StatusServiceUnavailable,
			)
			return
		}

		// Antwortformat bekanntgeben.
		writer.Header().Set("Content-Type", "application/json")

		// Ohne vorherigen Fehlerstatus wird HTTP 200 gesendet.
		// _ ignoriert die Anzahl geschriebener Bytes.
		_, _ = writer.Write([]byte(`{"status":"UP"}`))
	})

	// HTTP-Server konfigurieren.
	server := &http.Server{
		Handler: mux,

		// Clients dürfen nicht unbegrenzt lange Header übertragen.
		ReadHeaderTimeout: 5 * time.Second,
	}

	// Gepufferter Kanal für einen Serverfehler.
	serverErrors := make(chan error, 1)

	// Goroutine: Server parallel zum RabbitMQ-Consumer ausführen.
	go func() {
		serverErrors <- server.Serve(listener)
	}()

	return server, serverErrors, nil
}
