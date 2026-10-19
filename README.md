# Paperless

Paperless ist ein modulares Dokumentenmanagementsystem im Rahmen von SWEN3. Dokumente werden über eine Angular-Weboberfläche verwaltet; eine Spring-Boot-API speichert Metadaten in PostgreSQL und Dateien in einem gemeinsamen Docker-Volume. Keycloak übernimmt die Anmeldung. PDF-Uploads erzeugen über RabbitMQ Aufträge für einen Worker.

Diese README basiert auf der Vorarbeit und beschreibt den aktuellen Implementierungsstand.

## Technologie-Stack

| Bereich | Technologie |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Maven Wrapper |
| Frontend | Angular 21.2, TypeScript, Nginx |
| Datenbank | PostgreSQL 17 |
| Messaging | RabbitMQ 4.3.2 |
| Authentifizierung | Keycloak 26.3, OpenID Connect, JWT |
| Worker | Go; zusätzlich vorhandenes Java-OCR-Modul |
| Tests | JUnit, JaCoCo, Go-Tests, Postman/Newman |
| Betrieb | Docker Compose |

## Projektstruktur

```text
paperless/
├── api/                   # REST-API, Services, Persistenz und Security
├── share/                 # Gemeinsame Java-Nachrichten und Queue-Konstanten
├── ocr/                   # Java-Worker, weiterhin Teil des Maven-Builds
├── ocr-go/                # Aktuell in Docker Compose verwendeter Go-Worker
├── webui/                 # Angular-Frontend und Nginx-Konfiguration
├── _docs/                 # OpenAPI, Datenmodell und Sprint-Dokumentation
├── _postman/              # API-Tests, Umgebungen und Testdateien
├── compose-infra.yaml     # PostgreSQL, RabbitMQ und Keycloak
├── docker-compose.yaml    # API, WebUI, Go-Worker und Newman
├── init.sql               # Initiales Datenbankschema
├── paperless-realm.json   # Keycloak-Realm für die lokale Entwicklung
├── .env.example           # Vorlage für lokale Konfiguration
└── pom.xml                # Maven-Multi-Modul-Projekt
```

## Aktueller Funktionsumfang

- Dokumente mit Datei hochladen, auflisten, umbenennen und löschen.
- Dokumentzugriff anhand des angemeldeten Benutzers einschränken.
- Tags erstellen, auflisten, löschen und Dokumenten zuweisen; je Dokument ist höchstens ein Tag vorgesehen.
- Dokumente nach Tag filtern und Tag-Statistiken abrufen.
- Uploads hinsichtlich Titel, Dateigröße und Content-Type validieren.
- Für PDF-Uploads Nachrichten in der Queue `document.uploaded` veröffentlichen.
- Nachrichten im Go-Worker empfangen, protokollieren und bestätigen.

Der Worker ist aktuell ein Platzhalter: Eine echte OCR-Engine und die Rückgabe extrahierten Texts sind noch nicht integriert. MinIO, Elasticsearch sowie ein GenAI-Worker mit Google Gemini gehören zur geplanten Erweiterung.

## Architektur und Upload-Ablauf

```text
Browser ──> Angular / Nginx ──> Spring-Boot-API ──> PostgreSQL
   │                                  │
   └──> Keycloak                      ├──> Dateispeicher (Docker-Volume)
                                      └──> RabbitMQ ──> Go-Worker
```

1. Der Benutzer meldet sich über Keycloak an; API-Aufrufe verwenden ein JWT.
2. Die API validiert den Multipart-Upload.
3. Die Datei wird unter `/tmp/paperless` im Volume `paperless-storage` gespeichert, die Metadaten in PostgreSQL.
4. Für den Content-Type `application/pdf` veröffentlicht die API einen Auftrag in `document.uploaded`.
5. Der Go-Worker verarbeitet den Auftrag als Platzhalter und bestätigt die Nachricht. Das Dateivolume ist im Worker schreibgeschützt eingebunden.

## Lokal starten

Voraussetzung ist Docker mit Docker Compose v2 und Unterstützung für Compose-`include`. Für den Containerbetrieb sind keine lokalen Java-, Node.js- oder Go-Installationen nötig.

Alle folgenden Befehle werden im Projekt-Root ausgeführt, sofern kein anderes Verzeichnis angegeben ist.

### 1. Konfiguration anlegen

Unter PowerShell, falls noch keine `.env` existiert:

```powershell
Copy-Item .env.example .env
```

Unter Bash/WSL entsprechend `cp .env.example .env` verwenden. In `.env` die Platzhalter `<change-me>` für Datenbank, RabbitMQ und Keycloak ersetzen. Eine vorhandene `.env` beibehalten.

### 2. Container bauen und starten

```bash
docker compose up -d --build
docker compose ps
```

Beim ersten Start werden Images gebaut sowie Datenbank und Keycloak-Realm initialisiert. Der Newman-Container bleibt für spätere Testaufrufe bereit; er führt beim Start keine Tests aus.

### 3. Dienste öffnen

Die Adressen entsprechen den Ports aus `.env.example`:

| Dienst | Adresse |
| --- | --- |
| Weboberfläche | http://localhost |
| API-Healthcheck | http://localhost:8081/health |
| Keycloak | http://localhost:9445 |
| RabbitMQ Management | http://localhost:15672 |
| PostgreSQL | `localhost:5432` |

Die Keycloak-Administrationsanmeldung verwendet `KEYCLOAK_ADMIN` und `KEYCLOAK_ADMIN_PASSWORD`, RabbitMQ verwendet `RABBITMQ_USER` und `RABBITMQ_PASSWORD` aus `.env`. Anwendungs- und Testbenutzer werden über den Realm-Import eingerichtet; Hinweise zum Testlogin stehen in der [Postman-Dokumentation](_postman/README.md).

Bei Änderungen der Ports müssen gegebenenfalls auch Frontend-OIDC-Konfiguration, Keycloak-Redirect-URIs und Postman-Umgebungen angepasst werden. Ein Realm-Import aktualisiert einen bereits vorhandenen Realm nicht automatisch.

### Logs und Beenden

```bash
docker compose logs -f api ocr-go
docker compose down
```

`docker compose down` erhält die benannten Volumes mit Datenbank, Dateien und Dienstzuständen. Der Zusatz `-v` würde diese Daten löschen.

## Build und Tests

### Java-Module

Für einen lokalen Build wird JDK 25 benötigt. Maven wird über den Wrapper bereitgestellt.

```powershell
.\mvnw.cmd clean verify
```

Unter Bash/WSL:

```bash
./mvnw clean verify
```

Der Reactor baut `share`, `api` und `ocr`. JaCoCo ist an die Phase `verify` gebunden; der API-Bericht wird unter `api/target/site/jacoco/index.html` erzeugt. Der Go-Worker wird separat gebaut und getestet.

### Go-Worker

Im Verzeichnis `ocr-go` mit einer Go-Version gemäß `go.mod`:

```bash
go test ./...
go vet ./...
go build -o ocr-worker ./cmd/worker
```

Weitere Befehle und Coverage-Hinweise stehen in der [Worker-README](ocr-go/README.md).

### Frontend

Für lokale Frontend-Builds kann Node.js 24 wie im Dockerfile verwendet werden. Im Verzeichnis `webui`:

```bash
npm ci
npm run build
npm test -- --watch=false
```

### API-Integrationstests

Nach dem Start der Container unter Bash/WSL:

```bash
bash tests-newman.sh
```

Alternativ direkt aus PowerShell oder Bash:

```bash
docker compose exec -T newman newman run /etc/newman/postman.json -e /etc/newman/docker-environment.json
```

Die Collection meldet den Testbenutzer bei Keycloak an und prüft anschließend API-Anfragen. Import, Testdaten und einzelne Testordner sind in der [Postman-Dokumentation](_postman/README.md) beschrieben. Für einen manuellen Queue-Nachweis ist im aktuellen Compose-Setup der Service `ocr-go` zu stoppen und anschließend wieder zu starten.

## API und weitere Dokumentation

Die fachlichen API-Endpunkte erfordern Authentifizierung. `/health` ist öffentlich erreichbar.

| Pfad | Funktion |
| --- | --- |
| `/api/v1/documents` | Dokumente auflisten und hochladen |
| `/api/v1/documents/{id}` | Dokument abrufen, Titel ändern oder Dokument löschen |
| `/api/v1/documents/{id}/tag` | Tag-Zuordnung ändern oder entfernen |
| `/api/v1/tags` | Tags auflisten und erstellen |
| `/api/v1/tags/{id}` | Tag löschen |
| `/api/v1/statistics/tags` | Tag-Statistiken abrufen |
| `/api/v1/config/validation` | Validierungsregeln abrufen |

- [OpenAPI-Spezifikation](_docs/openapi.yaml)
- [Datenmodell als PlantUML](_docs/db.puml)
- [Datenbankschema](init.sql)
- [Sprint-Details](_docs/sprint-details.yaml)

## Geplante Erweiterungen

Die Zielarchitektur aus der Vorarbeit ergänzt den bestehenden Ablauf um echte OCR-Verarbeitung, Dateispeicherung in MinIO, Volltextsuche mit Elasticsearch und KI-Zusammenfassungen über einen GenAI-Worker mit Google Gemini. Diese Komponenten sind im aktuellen Compose-Setup noch nicht enthalten.
