package at.fhtw.swen.paperless.api.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
public record TagDto(
        UUID id,
        String name
) {
}
