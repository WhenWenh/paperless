package worker

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestElasticsearchIndexMappingAndDocument(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.Method + " " + r.URL.Path {
		case "HEAD /documents":
			w.WriteHeader(http.StatusNotFound)
		case "PUT /documents":
			var body struct {
				Mappings struct {
					Properties map[string]struct{ Type string }
				}
			}
			if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
				t.Error(err)
			}
			fields := body.Mappings.Properties
			if fields["content"].Type != "text" || fields["ownerId"].Type != "keyword" || fields["tagId"].Type != "keyword" {
				t.Errorf("incompatible index mapping: %+v", fields)
			}
			w.WriteHeader(http.StatusBadRequest)
			_, _ = io.WriteString(w, `{"error":{"type":"resource_already_exists_exception"}}`)
		case "PUT /documents/_doc/" + testDocumentID:
			document := make(map[string]any)
			if err := json.NewDecoder(r.Body).Decode(&document); err != nil {
				t.Error(err)
			}
			if document["documentId"] != testDocumentID || document["content"] != "Recognized text" ||
				document["ownerId"] != "20000000-0000-4000-8000-000000000001" ||
				document["originalFilename"] != "sample.pdf" || document["tagId"] != nil {
				t.Errorf("unexpected indexed document: %+v", document)
			}
			w.WriteHeader(http.StatusCreated)
		default:
			t.Errorf("unexpected request %s %s", r.Method, r.URL.Path)
			w.WriteHeader(http.StatusBadRequest)
		}
	}))
	defer server.Close()

	indexer := documentIndexer{client: server.Client(), indexURL: server.URL + "/documents"}
	if err := indexer.ensureIndex(t.Context()); err != nil {
		t.Fatal(err)
	}
	if err := indexer.index(t.Context(), ocrCompletedMessage{
		DocumentID:       testDocumentID,
		OwnerID:          new("20000000-0000-4000-8000-000000000001"),
		OriginalFilename: new("sample.pdf"),
		Text:             new("Recognized text"),
	}); err != nil {
		t.Fatal(err)
	}
}
