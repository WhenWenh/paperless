package worker_test

import (
	"errors"
	"net"
	"net/http"
	"net/http/httptest"
	"paperless/ocr-go/internal/worker"
	"strings"
	"testing"
	"time"
)

func TestHealthReadinessAndShutdown(t *testing.T) {
	ready := true
	server, stopped, err := worker.StartHealthServer("127.0.0.1:0", func() bool { return ready })
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { server.Close() })
	for _, tc := range []struct {
		ready  bool
		status int
		body   string
	}{{true, 200, `{"status":"UP"}`}, {false, 503, "RabbitMQ unavailable"}} {
		ready = tc.ready
		response := httptest.NewRecorder()
		server.Handler.ServeHTTP(response, httptest.NewRequest("GET", "/health", nil))
		if response.Code != tc.status || !strings.Contains(response.Body.String(), tc.body) {
			t.Fatalf("response: %d %s", response.Code, response.Body.String())
		}
		if ready && response.Header().Get("Content-Type") != "application/json" {
			t.Fatal("missing JSON content type")
		}
	}
	server.Close()
	select {
	case err := <-stopped:
		if !errors.Is(err, http.ErrServerClosed) {
			t.Fatalf("shutdown: %v", err)
		}
	case <-time.After(time.Second):
		t.Fatal("server did not stop")
	}
}

func TestHealthOccupiedPort(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	server, events, err := worker.StartHealthServer(listener.Addr().String(), func() bool { return true })
	if err == nil || server != nil || events != nil {
		t.Fatal("occupied port must fail cleanly")
	}
}
