package main

const documentUploadQueue = "document.uploaded"

type DocumentUploadedMessage struct {
	Title       string `json:"title"`
	Filename    string `json:"filename"`
	ContentType string `json:"contentType"`
	DocumentID  string `json:"documentId"`
	StorageUUID string `json:"storageUUID"`
}
