FROM golang:1.26-alpine AS build

ENV GOTOOLCHAIN=local

WORKDIR /src

COPY go.mod go.sum ./

RUN go mod download

COPY . .

RUN go test ./...

RUN CGO_ENABLED=0 go build -trimpath -o /out/ocr-worker ./cmd/worker

FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates poppler-utils tesseract-ocr tesseract-ocr-eng wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /out/ocr-worker /app/ocr-worker

EXPOSE 8082

ENTRYPOINT ["/app/ocr-worker"]
