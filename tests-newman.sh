#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
docker compose exec -T newman \
  newman run \
    /etc/newman/postman.json \
    -e /etc/newman/docker-environment.json \
    "$@"
