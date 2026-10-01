#!/usr/bin/env bash

REPORT="$(pwd)/api/target/site/jacoco/index.html"

if [ ! -f "$REPORT" ]; then
    echo "Coverage report not found: $REPORT"
    exit 1
fi

/home/mes/IDEA/idea-IU-253.31033.145/bin/idea.sh "$REPORT"