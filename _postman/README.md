# Postman und Newman

Die Struktur entspricht der Vorarbeit: Collection, Umgebungen, Testdateien und Dockerfile liegen in `_postman`. Die bestehenden CRUD- und sechs OCR-Tests bleiben enthalten. Ein neuer Ordner `Authentication` meldet den Entwicklungs-Testbenutzer bei Keycloak an und speichert dessen Access-Token für die folgenden Anfragen.

Die voreingestellten Zugangsdaten stammen aus dem vorhandenen lokalen Realm-Import. Bei nachträglich geänderten Keycloak-Zugangsdaten müssen `username`, `password` oder `clientSecret` entsprechend überschrieben werden. Keine produktiven Zugangsdaten oder echten Tokens committen. Ein Import des Realms aktualisiert eine bereits existierende Realm-Datenbank nicht automatisch.

## Docker / WSL

Im Projektverzeichnis:

```bash
docker compose up -d --build newman
bash tests-newman.sh
```

Newman läuft in einem eigenen, dauerhaft gestarteten Container. Es braucht keine lokale Newman-Installation. Der Container verwendet `http://api:8081` und `http://keycloak:8080` im Compose-Netzwerk. Compose startet die Abhängigkeiten und wartet auf deren Healthchecks. `tail -f /dev/null` hält den Container bereit, ohne Tests auszuführen. Das Skript startet die Tests mit `docker compose exec -T newman newman run ...` im laufenden Container. Nach dem Testlauf bleibt der Container bereit; der Exit-Code des Testlaufs wird weitergegeben. Auch ein gewöhnliches `docker compose up -d` startet den Newman-Service mit. Mit `docker compose stop newman` lässt er sich stoppen.

Zusätzliche Newman-Argumente werden weitergegeben. Bewusst kein `--verbose` als Standard, damit das Login-Passwort und die Token-Antwort nicht als Request-/Response-Body ausgegeben werden.

Nur Login und OCR-Tests:

```bash
bash tests-newman.sh --folder Authentication --folder "OCR Testing"
```

Der vollständige Lauf enthält Cleanup für die erzeugten Dokumente. Ein Lauf nur des OCR-Ordners lässt seine beiden erfolgreichen Uploads bestehen. Im Postman-Desktop lassen sie sich anschließend mit den beiden passenden OCR-DELETE-Anfragen unter Cleanup entfernen. Die Queue wird durch DELETE nicht geleert.

## Postman-Desktop

1. `_postman/postman.json` als vollständige Collection importieren.
2. `_postman/local-environment.json` importieren und als Umgebung auswählen.
3. Postmans Arbeitsverzeichnis auf den absoluten Pfad des Ordners `_postman` setzen, alternativ die Dateien unter Body/form-data manuell auswählen. Dateien sind nicht in der JSON eingebettet.
4. Zuerst `Authentication → Login as test user` senden. Danach CRUD- oder OCR-Anfragen senden. Beim vollständigen Collection-Run geschieht das automatisch.
5. Bei abgelaufenem Token die Login-Anfrage erneut ausführen. Die API-Anfragen erben Bearer-Authentifizierung von der Collection.

`docker-environment.json` ist für den Container, `local-environment.json` für den Desktop. `gitlab-environment.json` übernimmt die Host-Adressen aus der Vorarbeit als Vorlage; damit wird keine CI-Pipeline eingerichtet. Bei abweichenden Host-Ports die lokale Umgebung anpassen.

## OCR-Tests und Queue-Nachweis

| Anfrage | HTTP | Zusätzlicher OCR-Auftrag |
| --- | --- | --- |
| PDF | 201 | 1 |
| PNG | 201 | 0 |
| Metadaten ohne Datei | 400 | 0 |
| JSON ohne Multipart | 415 | 0 |
| Leere Datei | 400 | 0 |
| Ungültiger Titel | 400 | 0 |

Die Collection prüft HTTP-Antworten. Für den separaten Queue-Nachweis alle Consumer stoppen und währenddessen keine anderen Uploads ausführen:

```bash
docker compose stop ocr
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
bash tests-newman.sh --folder Authentication --folder "OCR Testing"
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
docker compose start ocr
```

Bei `consumers = 0` soll `document.uploaded` nach dem OCR-Lauf genau eine bereite Nachricht mehr enthalten als vorher. Der Newman-Service hängt deshalb nicht vom OCR-Worker ab. Ein vollständiger CRUD-Lauf erzeugt weitere PDF-Aufträge und eignet sich nicht für diesen exakten Vergleich.
