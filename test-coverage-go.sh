#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/ocr-go"
mkdir -p coverage

# Tests live in a separate package, so instrument the worker explicitly.
go test -count=1 -coverpkg=./internal/worker -coverprofile=coverage/coverage.out ./...
go tool cover -html=coverage/coverage.out -o coverage/coverage.html
echo "Coverage: $(pwd)/coverage/coverage.html"
