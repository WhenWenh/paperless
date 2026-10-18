package main

import "testing" // Go-Testwerkzeug, grob vergleichbar mit JUnit.

// Testfunktionen beginnen mit Test und erhalten *testing.T.
func TestDecodeMessageValid(t *testing.T) {
	// JSON im gleichen Format wie die Java-API.
	body := []byte(`{
		"title": "Invoice",
		"filename": "invoice.pdf",
		"contentType": "application/pdf",
		"documentId": "217df5bc-661c-4c0b-843f-93bdd1273e94",
		"storageUuid": "1e195006-626f-4437-b913-9b8358607ef9"
	}`)

	// Funktion unter Test ausführen.
	message, err := decodeMessage(body)

	// Bei Fehler diesen Test sofort abbrechen.
	if err != nil {
		t.Fatalf("expected valid message, got %v", err)
	}

	// Prüfen, ob das JSON-Feld korrekt zugeordnet wurde.
	if message.DocumentID != "217df5bc-661c-4c0b-843f-93bdd1273e94" {
		t.Fatalf("unexpected document ID: %s", message.DocumentID)
	}

	if message.StorageUUID != "1e195006-626f-4437-b913-9b8358607ef9" {
		t.Fatalf("unexpected storage UUID: %s", message.StorageUUID)
	}
}

func TestDecodeMessageMissingDocumentID(t *testing.T) {
	// Dokument-ID absichtlich weglassen.
	body := []byte(`{
		"contentType": "application/pdf",
		"storageUuid": "1e195006-626f-4437-b913-9b8358607ef9"
	}`)

	// Der gelesene Nachrichtenwert interessiert hier nicht: _.
	_, err := decodeMessage(body)

	// Ohne Fehler wäre dieser Test fehlgeschlagen.
	if err == nil {
		t.Fatal("expected an error for missing documentId")
	}
}

func TestDecodeMessageInvalidJSON(t *testing.T) {
	// Absichtlich unvollständiges JSON.
	_, err := decodeMessage([]byte(`{"documentId":`))

	if err == nil {
		t.Fatal("expected an error for invalid JSON")
	}
}
