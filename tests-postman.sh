#!/bin/bash
cd "$(dirname "$0")" || exit 1
newman run ./postman.json --verbose