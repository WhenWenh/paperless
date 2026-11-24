# GenAI-Worker in Go

Der Worker liest OCR-Ergebnisse aus `document.ocr.completed` und indexiert
sie anhand der Dokument-ID in Elasticsearch (`documents`). Für vorhandenen
OCR-Text erstellt er anschließend eine Zusammenfassung und sendet sie über
`document.summary.generated` an den Java-API-Listener. Der Listener speichert
die Zusammenfassung in PostgreSQL.

## Anbieter auswählen

Ollama ist der Standard. Die Einstellungen in der `.env` des Projekts
werden bei einem Compose-Start an den Worker übergeben.

| Variable | Einstellung |
| --- | --- |
| `GENAI_PROVIDER` | `ollama` oder `gemini`, Standard `ollama` |
| `OLLAMA_BASE_URL` | In Compose standardmäßig `http://host.docker.internal:11434`. Beim direkten Start standardmäßig `http://localhost:11434`. |
| `OLLAMA_MODEL` | Standard `qwen2.5:latest`. Das gewählte Modell muss in Ollama vorhanden sein. |
| `GOOGLE_API_KEY` | Für Gemini erforderlich |
| `GEMINI_MODEL` | Standard `gemini-3.5-flash-lite` |

Wenn Ollama auf dem Host läuft, muss es über die in `OLLAMA_BASE_URL`
angegebene Adresse aus dem Container erreichbar sein. Nach einem
Anbieterwechsel in `.env` erstellt `docker compose up -d genai-go` den
Container mit der geänderten Konfiguration neu.

## Betrieb mit Docker Compose

`genai-go` ist in der Compose-Datei aktiv. Start im Projektverzeichnis.

```bash
docker compose up -d --build genai-go
docker compose logs -f ocr-go genai-go api
```

Beim Wechsel zuerst den laufenden GenAI-Worker stoppen, dann seinen
Serviceblock auskommentieren und den anderen einkommentieren. Beide Worker
lesen dieselbe RabbitMQ-Queue.

Der Healthcheck auf Port `8083` prüft RabbitMQ und Elasticsearch. Auch
wenn der Modellanbieter offline ist, kann der Worker OCR-Text indexieren.
Vorübergehende Generierungsfehler werden bis zu dreimal versucht.
Endgültig fehlgeschlagene Zusammenfassungen landen in
`document.ocr.completed.dlq`. Dokumente mit leerem OCR-Text werden
indexiert und ohne Zusammenfassung quittiert.

## Lokale Entwicklung

Im Ordner `genai-go` mit Go 1.26 oder neuer ausführen.

```bash
go test -count=1 ./...
go vet ./...
go build -o bin/genai-worker ./cmd/worker
```

Für einen direkten Start erwartet der Worker `RABBITMQ_USER` und
`RABBITMQ_PASSWORD` in der Umgebung. RabbitMQ verwendet standardmäßig
`localhost:5672` und Elasticsearch `http://localhost:9200`. Die Werte
lassen sich über `RABBITMQ_HOST`, `RABBITMQ_PORT` und `ELASTICSEARCH_URL`
überschreiben.
