#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

REPORT="$(pwd)/api/target/site/jacoco/index.html"

if [ ! -f "$REPORT" ]; then
    echo "Coverage-Bericht fehlt. Zuerst ausführen:"
    echo "./mvnw -pl api -am clean verify"
    exit 1
fi

explorer.exe "$(wslpath -w "$REPORT")"