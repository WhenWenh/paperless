# OCR-Worker (Go)

Empfängt PDF-Aufträge aus der RabbitMQ-Queue `document.uploaded`,
protokolliert die Dokument-ID und bestätigt erfolgreich verarbeitete
Nachrichten. Eine echte OCR-Engine ist noch nicht integriert.

## Voraussetzungen

- Go **1.27.2 oder neuer**, entsprechend `go.mod`
- Docker und Docker Compose für den Containerbetrieb

Installierte Go-Version prüfen:

```bash
go version
```

## Entwicklung

Alle Go-Befehle im Verzeichnis `ocr-go` ausführen:

```bash
cd ocr-go
```

| Maven / Aufgabe | Go-Befehl |
|---|---|
| Abhängigkeiten herunterladen | `go mod download` |
| Abhängigkeiten mit dem Code abgleichen | `go mod tidy` |
| `mvn compile` / Anwendung bauen | `go build -o ocr-worker ./cmd/worker` |
| `mvn test` | `go test ./...` |
| Tests ohne zwischengespeichertes Ergebnis | `go test -count=1 ./...` |
| Zusätzliche Codeprüfung | `go vet ./...` |
| Code formatieren | `go fmt ./...` |
| Lokale Anwendung starten | `go run ./cmd/worker` |

Für einen frischen Test- und Buildlauf:

```bash
go test -count=1 ./...
go vet ./...
go build -o ocr-worker ./cmd/worker
```

Ein regelmäßiges `clean` ist normalerweise nicht nötig. Bei Bedarf:

```bash
go clean -testcache
rm -f ./ocr-worker
```

## Coverage

```bash
go test ./... -coverprofile=coverage.out
go tool cover -html=coverage.out -o coverage.html
```

Anschließend `coverage.html` im Browser öffnen.

