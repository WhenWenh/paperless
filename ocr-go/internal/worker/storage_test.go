package worker

import (
	"context"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestS3StorageUsesBucketAndStorageUUID(t *testing.T) {
	const storageUUID = "1e195006-626f-4437-b913-9b8358607ef9"
	requests := make(chan string, 10)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requests <- r.Method + " " + r.URL.Path
		if !strings.HasPrefix(r.Header.Get("Authorization"), "AWS4-HMAC-SHA256 ") {
			w.WriteHeader(http.StatusForbidden)
			return
		}
		switch r.Method + " " + r.URL.Path {
		case "HEAD /paperless":
			w.WriteHeader(http.StatusNotFound)
		case "PUT /paperless":
			w.WriteHeader(http.StatusOK)
		case "GET /paperless/" + storageUUID:
			_, _ = io.WriteString(w, "stored document")
		default:
			w.WriteHeader(http.StatusBadRequest)
		}
	}))
	defer server.Close()

	ctx := context.Background()
	openDocument, err := openStorage(ctx, Config{
		StorageType: "s3", StorageEndpoint: server.URL, StorageRegion: "us-east-1",
		StorageBucket: "paperless", StorageAccessKey: "test-key", StorageSecretKey: "test-secret",
	})
	if err != nil {
		t.Fatal(err)
	}
	result, err := openDocument(ctx, storageUUID)
	if err != nil {
		t.Fatal(err)
	}
	defer result.Close()
	body, err := io.ReadAll(result)
	if err != nil || string(body) != "stored document" || len(requests) != 3 {
		t.Fatalf("body = %q, error = %v, request count = %d", body, err, len(requests))
	}
}
