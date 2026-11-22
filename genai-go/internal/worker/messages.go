package worker

import (
	"encoding/json"
	"errors"
	"regexp"
	"strings"
)

const (
	ocrCompletedQueue           = "document.ocr.completed"
	ocrCompletedDeadLetterQueue = "document.ocr.completed.dlq"
	summaryGeneratedQueue       = "document.summary.generated"
)

type ocrCompletedMessage struct {
	DocumentID       string  `json:"documentId"`
	StorageUUID      *string `json:"storageUuid"`
	OwnerID          *string `json:"ownerId"`
	Title            *string `json:"title"`
	OriginalFilename *string `json:"originalFilename"`
	TagID            *string `json:"tagId"`
	ContentType      string  `json:"contentType"`
	Text             *string `json:"text"`
}

type summaryGeneratedMessage struct {
	DocumentID string `json:"documentId"`
	Summary    string `json:"summary"`
}

var uuidPattern = regexp.MustCompile(`(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$`)

func decodeOcrResult(body []byte) (ocrCompletedMessage, error) {
	var message ocrCompletedMessage
	if err := json.Unmarshal(body, &message); err != nil {
		return message, errors.New("invalid OCR result JSON")
	}
	if !uuidPattern.MatchString(message.DocumentID) {
		return message, errors.New("documentId must be a UUID")
	}
	for _, id := range []*string{message.StorageUUID, message.OwnerID, message.TagID} {
		if id != nil {
			if !uuidPattern.MatchString(*id) {
				return message, errors.New("storageUuid, ownerId and tagId must be UUIDs when present")
			}
			*id = strings.ToLower(*id)
		}
	}
	message.DocumentID = strings.ToLower(message.DocumentID)
	return message, nil
}
