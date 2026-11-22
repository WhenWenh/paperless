# OCR-Worker (Go)

Empfängt Aufträge aus `document.uploaded` und lädt die Datei anhand der
`storageUuid` aus libreFS (S3) oder aus `/tmp/paperless`. Der Worker rendert
PDF-Seiten mit Poppler und erkennt ihren Text mit Tesseract. PNG, JPEG, TIFF,
BMP, GIF und WebP gehen direkt an Tesseract. Die Upload-API stellt derzeit
PDF-Aufträge in die Queue.

Der Worker sendet Text und Metadaten an `document.ocr.completed`. Der
`genai-go`-Dienst verwendet dieses Ergebnis für Suche und
Zusammenfassung. Der Worker bestätigt den Auftrag, sobald RabbitMQ die
Veröffentlichung bestätigt hat.

## Voraussetzungen

- Go **1.26 oder neuer**, entsprechend `go.mod`
- Docker und Docker Compose für den Containerbetrieb
- Für lokalen OCR-Betrieb `pdftoppm` und `tesseract` mit englischen Sprachdaten

## Betrieb mit Docker Compose

`ocr-go` ist in der Compose-Datei aktiv. Start im Projekt-Root.

```bash
docker compose up -d --build ocr-go
docker compose logs -f ocr-go genai-go
```

Der Service übernimmt die `STORAGE_*`-Variablen aus der Projektkonfiguration.
Für S3 sind `STORAGE_ACCESS_KEY` und `STORAGE_SECRET_KEY` erforderlich. Der
Healthcheck ist im Container unter `http://localhost:8082/health` erreichbar.

Beim Wechsel zuerst den laufenden OCR-Worker stoppen. Dann seinen
Serviceblock in `docker-compose.yaml` auskommentieren, den anderen
Serviceblock einkommentieren und diesen mit `docker compose up -d --build`
starten. Die beiden Worker lesen aus derselben RabbitMQ-Queue.

Installierte Go-Version prüfen:

```bash
go version
```

## Entwicklung

```text
ocr-go/
├── cmd/worker/         # Programmeinstieg (main.go)
├── internal/worker/    # Importierbare Worker-Logik
├── test/               # Go-Tests als separates Paket
├── coverage/           # Generierte Testberichte (ignoriert)
└── bin/                # Lokale Build-Ausgaben (ignoriert)
```

Die Tests importieren `paperless/ocr-go/internal/worker`. `go test ./...`
findet auch die Tests unter `test/`; Docker verwendet denselben Befehl.

Alle Go-Befehle im Verzeichnis `ocr-go` ausführen:

```bash
cd ocr-go
```

| Maven / Aufgabe | Go-Befehl |
|---|---|
| Abhängigkeiten herunterladen | `go mod download` |
| Abhängigkeiten mit dem Code abgleichen | `go mod tidy` |
| `mvn compile` / Anwendung bauen | `go build -o bin/ocr-worker ./cmd/worker` |
| `mvn test` | `go test ./...` |
| Tests ohne zwischengespeichertes Ergebnis | `go test -count=1 ./...` |
| Zusätzliche Codeprüfung | `go vet ./...` |
| Code formatieren | `go fmt ./...` |
| Lokale Anwendung starten | `go run ./cmd/worker` |

Für einen frischen Test- und Buildlauf:

```bash
go test -count=1 ./...
go vet ./...
go build -o bin/ocr-worker ./cmd/worker
```

Ein regelmäßiges `clean` ist normalerweise nicht nötig. Bei Bedarf:

```bash
go clean -testcache
rm -f ./bin/ocr-worker
```

## Coverage

```bash
mkdir -p coverage
go test -count=1 -coverpkg=./internal/worker -coverprofile=coverage/coverage.out ./...
go tool cover -html=coverage/coverage.out -o coverage/coverage.html
```

`-coverpkg` erfasst die Worker-Logik, obwohl die Tests in einem eigenen Paket
liegen. Gemessen wird `internal/worker`; der Programmeinstieg ist ausgenommen.
Die Berichte werden nicht versioniert. Alternativ im Projekt-Root
`bash test-coverage-go.sh` ausführen; das Skript wechselt selbst nach `ocr-go`.

Anschließend `coverage/coverage.html` im Browser öffnen (WSL):
```bash
explorer.exe "$(wslpath -w "$PWD/coverage/coverage.html")"
```
