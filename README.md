# Paperless

Paperless ist ein modulares Dokumentenmanagementsystem im Rahmen von SWEN3. Dokumente werden über eine Angular-Weboberfläche verwaltet; eine Spring-Boot-API speichert Metadaten in PostgreSQL und Originaldateien in einem S3-kompatiblen Objektspeicher. Keycloak übernimmt die Anmeldung. Java-Worker verarbeiten PDF-Uploads asynchron über RabbitMQ: Tesseract extrahiert den Text, Elasticsearch stellt ihn für die Inhaltssuche bereit.

## Technologie-Stack

| Bereich | Technologie |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Maven Wrapper |
| Frontend | Angular 21.2, TypeScript, Nginx |
| Datenbank | PostgreSQL 17 |
| Messaging | RabbitMQ 4.3.2 |
| Authentifizierung | Keycloak 26.3, OpenID Connect, JWT |
| Worker | Java/Spring Boot: OCR und Indexierung |
| OCR | Tesseract, Poppler (`pdftoppm`) |
| Objektspeicher | S3-Schnittstelle mit libreFS, einem MinIO-Fork |
| Suchindex | Elasticsearch 9.4.3 |
| Tests | JUnit, JaCoCo, Go-Tests, Postman/Newman |
| Betrieb | Docker Compose |

## Projektstruktur

```text
paperless/
├── api/                   # REST-API, Services, Persistenz und Security
├── share/                 # Gemeinsame Java-Nachrichten und Queue-Konstanten
├── ocr/                   # Java-OCR-Worker mit S3-Zugriff und Tesseract
├── genai/                 # Java-Indexierungs-Worker für Elasticsearch
├── ocr-go/                # Separates Go-Worker-Modul mit eigenen Tests
├── _filestore/            # Dockerfile für den S3-kompatiblen Objektspeicher
├── webui/                 # Angular-Frontend und Nginx-Konfiguration
├── _docs/                 # OpenAPI, Datenmodell und Sprint-Dokumentation
├── _postman/              # API-Tests, Umgebungen und Testdateien
├── compose-infra.yaml     # PostgreSQL, RabbitMQ, Keycloak, Dateispeicher, Elasticsearch
├── docker-compose.yaml    # API, WebUI, Java-Worker und Newman
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
- Originaldateien unter eindeutigen Objektschlüsseln im S3-Speicher ablegen und den Originaldateinamen als Metadatum erhalten.
- PDF-Dateien im Java-OCR-Worker aus dem Objektspeicher laden und seitenweise mit Tesseract erkennen.
- OCR-Ergebnisse über `document.ocr.completed` an den Indexierungs-Worker übergeben.
- OCR-Texte im Elasticsearch-Index `documents` unter der Dokument-ID speichern und bei erneuter Indexierung derselben ID aktualisieren.
- Dokumentinhalte über die Suchseite abfragen und Treffer mit Titel und Link zur Detailseite anzeigen.
- Ladezustand, leere Suchergebnisse und Suchfehler in der Weboberfläche darstellen.

## Architektur und Upload-Ablauf

```text
Browser ──> Angular / Nginx ──> Spring-Boot-API ──> PostgreSQL
   │                                  │
   └──> Keycloak                      ├──> S3-Objektspeicher (filestore)
                                      ├──> Elasticsearch (Suche)
                                      └──> RabbitMQ: document.uploaded
                                                    │
                                                    v
                                               OCR-Worker
                                               │ S3-Download
                                               │ Poppler + Tesseract
                                               v
                                      RabbitMQ: document.ocr.completed
                                                    │
                                                    v
                                         Indexierungs-Worker (genai)
                                                    │
                                                    v
                                               Elasticsearch
```

1. Der Benutzer meldet sich über Keycloak an; API-Aufrufe verwenden ein JWT.
2. Die API validiert den Multipart-Upload und speichert die Datei über den S3-Client im konfigurierten Bucket.
3. Eine neu erzeugte `storageUuid` dient als Objektschlüssel. PostgreSQL speichert die Dokument-ID, den Eigentümer, die Speicherreferenz und weitere Metadaten einschließlich Originaldateiname und Tag-Zuordnung. Gleichnamige Uploads erhalten unterschiedliche Objektschlüssel.
4. Für den Content-Type `application/pdf` veröffentlicht die API einen Auftrag in `document.uploaded`.
5. Der OCR-Worker empfängt die Nachricht und lädt die tatsächlichen Dateidaten anhand der `storageUuid` aus dem Objektspeicher in eine lokale temporäre Datei.
6. Poppler wandelt die PDF-Seiten mit `pdftoppm -png` in Bilder um. Tesseract erkennt den Text jeder Seite; der Worker fügt die Ergebnisse in Seitenreihenfolge zusammen.
7. Der Worker veröffentlicht den Text zusammen mit Dokument-ID, Speicherreferenz und Metadaten in `document.ocr.completed`. Verarbeitungsfehler werden mit Dokument-ID und Speicherreferenz protokolliert und als Fehler weitergegeben.
8. Der Indexierungs-Worker empfängt das Ergebnis und speichert es über Spring Data Elasticsearch im Index `documents`. Die Dokument-ID ist zugleich die Elasticsearch-ID; erneutes Speichern derselben ID aktualisiert den Eintrag.
9. Die Suchseite ruft `/api/v1/documents/search` auf. Die API sucht im indexierten OCR-Text für den angemeldeten Benutzer, lädt die zugehörigen Dokumentdaten aus PostgreSQL und wendet die gewählten Tag-Filter an.

## Worker und Speicher im Containerbetrieb

### Eigenständige Worker-Anwendungen

`ocr` und `genai` sind eigene Spring-Boot-Anwendungen mit separaten Dockerfiles. Das Modul `share` enthält die gemeinsamen Nachrichtenmodelle und Queue-Namen. Der Compose-Service `ocr` übernimmt die Texterkennung; der Service `genai` übernimmt die Indexierung der OCR-Ergebnisse. RabbitMQ verbindet die Verarbeitungsschritte asynchron.

### Tesseract und PDF-Konvertierung

Das OCR-Image installiert `tesseract-ocr`, die englischen Sprachdaten `tesseract-ocr-eng` und `poppler-utils`. `pdftoppm` übernimmt die PDF-Konvertierung, Tesseract liefert den erkannten Text über die Standardausgabe. Das für Konvertierung und Texterkennung erzeugte Arbeitsverzeichnis wird anschließend aufgeräumt. Auch ein leerer erkannter Text kann als Ergebnis weitergegeben werden.

### S3-Dateispeicher mit MinIO-kompatibler Anbindung

Der REST-Server und der OCR-Worker verwenden den AWS-S3-Client. Der Compose-Service `filestore` startet libreFS aus dem Dockerfile unter `_filestore`; die S3-API ist im Container-Netzwerk unter `http://filestore:9000` erreichbar. Die Objektdaten liegen unter `/data` im persistenten Volume `filestore-data`.

API und OCR verwenden dieselben Speicherparameter aus `.env`:

| Variable | Zweck |
| --- | --- |
| `STORAGE_TYPE=s3` | S3-Speicherimplementierung auswählen |
| `STORAGE_ENDPOINT=http://filestore:9000` | Interne Adresse des Objektspeichers |
| `STORAGE_REGION=us-east-1` | Region für den S3-Client |
| `STORAGE_BUCKET=paperless` | Bucket für Originaldateien |
| `STORAGE_ACCESS_KEY` | Zugangsschlüssel; zugleich Root-Benutzer des Dateispeichers |
| `STORAGE_SECRET_KEY` | Geheimer Schlüssel; zugleich Root-Passwort des Dateispeichers |

### Elasticsearch und gemeinsamer Start

`compose-infra.yaml` definiert Elasticsearch im Single-Node-Modus mit persistentem Volume `elasticsearch-data` und Cluster-Healthcheck. API und Indexierungs-Worker greifen intern über `http://elasticsearch:9200` darauf zu. Der Index enthält OCR-Text, Dokument-ID, Eigentümer-ID, Titel, Originaldateiname und Tag-ID.

`docker-compose.yaml` bindet die Infrastrukturdatei über `include` ein und ergänzt API, WebUI, OCR-Worker, Indexierungs-Worker und Newman. Die Dienste kommunizieren über `paperless_network` mit Compose-Service-Namen wie `rabbitmq`, `postgres`, `filestore` und `elasticsearch`. Healthchecks und `depends_on` koordinieren den Start der abhängigen Dienste. Die Verarbeitung vom Upload bis zur Suche läuft über diese Container.

## Lokal starten

Voraussetzung ist Docker mit Docker Compose v2 und Unterstützung für Compose-`include`. Für den Containerbetrieb sind keine lokalen Java-, Node.js- oder Go-Installationen nötig.

Alle folgenden Befehle werden im Projekt-Root ausgeführt, sofern kein anderes Verzeichnis angegeben ist.

### 1. Konfiguration anlegen

Unter PowerShell, falls noch keine `.env` existiert:

```powershell
Copy-Item .env.example .env
```

Unter Linux/Bash/WSL entsprechend `cp .env.example .env` verwenden. In `.env` die Platzhalter `<change-me>` für Datenbank, RabbitMQ, Keycloak und den S3-Dateispeicher ersetzen. Eine vorhandene `.env` beibehalten.

### 2. Container bauen und starten

```bash
docker compose build
docker compose up -d --wait
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
| S3-API | http://localhost:9000 |
| Dateispeicher-Webkonsole | http://localhost:9001 |
| Elasticsearch | http://localhost:9200 |

Die Keycloak-Administrationsanmeldung verwendet `KEYCLOAK_ADMIN` und `KEYCLOAK_ADMIN_PASSWORD`, RabbitMQ verwendet `RABBITMQ_USER` und `RABBITMQ_PASSWORD` aus `.env`. Die Dateispeicher-Webkonsole verwendet `STORAGE_ACCESS_KEY` und `STORAGE_SECRET_KEY`. Anwendungs- und Testbenutzer werden über den Realm-Import eingerichtet; Hinweise zum Testlogin stehen in der [Postman-Dokumentation](_postman/README.md).

Bei Änderungen der Ports müssen gegebenenfalls auch Frontend-OIDC-Konfiguration, Keycloak-Redirect-URIs und Postman-Umgebungen angepasst werden. Ein Realm-Import aktualisiert einen bereits vorhandenen Realm nicht automatisch.

### Logs und Beenden

```bash
docker compose logs -f api ocr genai filestore elasticsearch
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

Der Reactor baut `share`, `api`, `ocr` und `genai`. JaCoCo ist an die Phase `verify` gebunden; der API-Bericht wird unter `api/target/site/jacoco/index.html` erzeugt. Der Go-Worker wird separat gebaut und getestet.

### Go-Worker

Im Verzeichnis `ocr-go` mit einer Go-Version gemäß `go.mod`:

```bash
go test ./...
go vet ./...
go build -o bin/ocr-worker ./cmd/worker
```

Weitere Befehle und Coverage-Hinweise stehen in der [Worker-README](ocr-go/README.md).

Die Go-Tests liegen unter `ocr-go/test/`, die getestete Worker-Logik unter
`ocr-go/internal/worker/`. Für Coverage im Projekt-Root
`bash test-coverage-go.sh` ausführen. Die Berichte landen unter
`ocr-go/coverage/`, lokale Go-Binaries unter `ocr-go/bin/`; beide Ordner
sind in `.gitignore` ausgeschlossen. IntelliJ-Dateien (`*.iml`) sind ebenfalls
ignoriert und werden bei Bedarf von der IDE aus dem Maven-Projekt erzeugt.

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

Die Collection meldet den Testbenutzer bei Keycloak an und prüft anschließend API-Anfragen. Import, Testdaten und einzelne Testordner sind in der [Postman-Dokumentation](_postman/README.md) beschrieben. Für einen manuellen Queue-Nachweis ist im aktuellen Compose-Setup der Service `ocr` zu stoppen und anschließend wieder zu starten.

## Verarbeitung und Suche prüfen

1. Über die Weboberfläche eine PDF mit einem gut lesbaren Begriff hochladen, beispielsweise `HelloWorld.pdf` mit dem Inhalt `Hello World`.
2. Die Dokument-ID aus der Upload-Antwort beziehungsweise der Detailseite notieren.
3. Mit `docker compose logs -f api ocr genai` die Verarbeitung verfolgen. Der OCR-Worker protokolliert den Empfang und das Laden des Dokuments sowie die erfolgreiche Verarbeitung. Der Indexierungs-Worker meldet `Successfully indexed document` mit derselben Dokument-ID.
4. Nach abgeschlossener Indexierung die Suchseite öffnen und nach `Hello` suchen. Der Treffer zeigt den Dokumenttitel und führt zur Detailseite.
5. Mit `No Tag` oder einem konkreten Tag die Ergebnisse filtern. Das Leeren des Suchfelds zeigt die eigenen Dokumente entsprechend der gewählten Filter an.

Der gespeicherte OCR-Text lässt sich zusätzlich direkt aus Elasticsearch abrufen:

```bash
DOCUMENT_ID='DOKUMENT-ID-AUS-DEM-UPLOAD'
curl -sS "http://localhost:9200/documents/_doc/$DOCUMENT_ID?pretty"
```

Die Antwort enthält im Feld `_source.content` den indexierten Text. Die Originaldatei ist in der Dateispeicher-Webkonsole anhand ihrer `storageUuid` auffindbar. Für diesen Ablauf das Testdokument bis zum Abschluss der Verarbeitung bestehen lassen.

## API und weitere Dokumentation

Die fachlichen API-Endpunkte erfordern Authentifizierung. `/health` ist öffentlich erreichbar.

| Pfad | Funktion |
| --- | --- |
| `/api/v1/documents` | Dokumente auflisten und hochladen |
| `/api/v1/documents/search` | OCR-Inhalte suchen und Ergebnisse nach Tag filtern |
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

