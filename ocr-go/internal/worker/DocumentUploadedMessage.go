package worker

const documentUploadQueue = "document.uploaded"
const ocrCompletedQueue = "document.ocr.completed"
const ocrCompletedDeadLetterQueue = "document.ocr.completed.dlq"

type DocumentUploadedMessage struct {
	Title       string  `json:"title"`
	Filename    string  `json:"filename"`
	ContentType string  `json:"contentType"`
	DocumentID  string  `json:"documentId"`
	StorageUUID string  `json:"storageUuid"`
	OwnerID     *string `json:"ownerId"`
	TagID       *string `json:"tagId"`
}

type OcrCompletedMessage struct {
	DocumentID       string  `json:"documentId"`
	StorageUUID      string  `json:"storageUuid"`
	OwnerID          *string `json:"ownerId"`
	Title            string  `json:"title"`
	OriginalFilename string  `json:"originalFilename"`
	TagID            *string `json:"tagId"`
	ContentType      string  `json:"contentType"`
	Text             string  `json:"text"`
}
