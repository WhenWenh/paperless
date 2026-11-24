package worker

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
)

const documentMapping = `{
  "mappings": {
    "properties": {
      "documentId": {"type": "keyword"},
      "content": {"type": "text"},
      "ownerId": {"type": "keyword"},
      "title": {"type": "text"},
      "originalFilename": {"type": "keyword"},
      "tagId": {"type": "keyword"}
    }
  }
}`

type documentIndexer struct {
	client   *http.Client
	indexURL string
}

func (i documentIndexer) ensureIndex(ctx context.Context) error {
	response, err := i.request(ctx, http.MethodHead, i.indexURL, nil)
	if err != nil {
		return fmt.Errorf("check Elasticsearch index: %w", err)
	}
	response.Body.Close()
	switch response.StatusCode {
	case http.StatusOK:
		return nil
	case http.StatusNotFound:
		// The Java API can also create the index during startup.
	default:
		return fmt.Errorf("check Elasticsearch index: HTTP %d", response.StatusCode)
	}

	response, err = i.request(ctx, http.MethodPut, i.indexURL, strings.NewReader(documentMapping))
	if err != nil {
		return fmt.Errorf("create Elasticsearch index: %w", err)
	}
	defer response.Body.Close()
	if response.StatusCode >= 200 && response.StatusCode < 300 {
		_, _ = io.Copy(io.Discard, io.LimitReader(response.Body, 4096))
		return nil
	}
	if response.StatusCode == http.StatusBadRequest {
		var result struct {
			Error struct {
				Type string `json:"type"`
			} `json:"error"`
		}
		if json.NewDecoder(io.LimitReader(response.Body, 16384)).Decode(&result) == nil &&
			result.Error.Type == "resource_already_exists_exception" {
			return nil
		}
	}
	return fmt.Errorf("create Elasticsearch index: HTTP %d", response.StatusCode)
}

func (i documentIndexer) index(ctx context.Context, message ocrCompletedMessage) error {
	document := struct {
		DocumentID       string  `json:"documentId"`
		Content          *string `json:"content"`
		OwnerID          *string `json:"ownerId"`
		Title            *string `json:"title"`
		OriginalFilename *string `json:"originalFilename"`
		TagID            *string `json:"tagId"`
	}{
		DocumentID:       message.DocumentID,
		Content:          message.Text,
		OwnerID:          message.OwnerID,
		Title:            message.Title,
		OriginalFilename: message.OriginalFilename,
		TagID:            message.TagID,
	}
	body, err := json.Marshal(document)
	if err != nil {
		return err
	}
	response, err := i.request(ctx, http.MethodPut, i.indexURL+"/_doc/"+message.DocumentID, bytes.NewReader(body))
	if err != nil {
		return err
	}
	defer response.Body.Close()
	_, _ = io.Copy(io.Discard, io.LimitReader(response.Body, 4096))
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return fmt.Errorf("index document: HTTP %d", response.StatusCode)
	}
	return nil
}

func (i documentIndexer) ready(ctx context.Context) bool {
	response, err := i.request(ctx, http.MethodHead, i.indexURL, nil)
	if err != nil {
		return false
	}
	response.Body.Close()
	return response.StatusCode == http.StatusOK
}

func (i documentIndexer) request(ctx context.Context, method, endpoint string, body io.Reader) (*http.Response, error) {
	request, err := http.NewRequestWithContext(ctx, method, endpoint, body)
	if err != nil {
		return nil, err
	}
	if body != nil {
		request.Header.Set("Content-Type", "application/json")
	}
	return i.client.Do(request)
}
