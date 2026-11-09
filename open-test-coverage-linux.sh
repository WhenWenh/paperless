#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

REPORT="${1:-$SCRIPT_DIR/api/target/site/jacoco/index.html}"

if [ ! -f "$REPORT" ]; then
    echo "Coverage-Bericht fehlt: $REPORT" >&2
    echo "Im Projektverzeichnis zuerst ausführen: bash mvnw -pl api -am clean verify" >&2
    exit 1
fi

if command -v wslpath >/dev/null 2>&1 && command -v explorer.exe >/dev/null 2>&1; then
    explorer.exe "$(wslpath -w "$REPORT")"
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$REPORT"
elif command -v gio >/dev/null 2>&1; then
    gio open "$REPORT"
else
    echo "Kein unterstützter Browser-Öffner gefunden. Bericht manuell öffnen: $REPORT" >&2
    exit 1
fi
