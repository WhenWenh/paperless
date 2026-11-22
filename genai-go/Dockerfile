FROM golang:1.26-alpine AS build

ENV GOTOOLCHAIN=local
WORKDIR /src

COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN go test ./...
RUN CGO_ENABLED=0 go build -trimpath -o /out/genai-worker ./cmd/worker

FROM alpine:3

RUN apk add --no-cache ca-certificates
COPY --from=build /out/genai-worker /usr/local/bin/genai-worker

EXPOSE 8083
ENTRYPOINT ["/usr/local/bin/genai-worker"]
