package at.fhtw.swen.paperless.api.controller.response;

import java.time.Instant;

public record HealthResponse(String status, Instant timestamp) {
}
