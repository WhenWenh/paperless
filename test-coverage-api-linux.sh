#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

# Wie bisher den vorhandenen API-Bericht öffnen, jetzt ohne festen IDE-Pfad.
exec bash "$SCRIPT_DIR/open-test-coverage-linux.sh" "$@"
