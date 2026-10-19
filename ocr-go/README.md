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
