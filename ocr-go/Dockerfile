FROM golang:1-alpine AS build

WORKDIR /src

COPY go.mod go.sum ./

RUN go mod download

COPY . .

RUN go test ./...

RUN CGO_ENABLED=0 go build -trimpath -o /out/ocr-worker ./cmd/worker

FROM alpine:3

WORKDIR /app

COPY --from=build /out/ocr-worker /app/ocr-worker

EXPOSE 8082

ENTRYPOINT ["/app/ocr-worker"]