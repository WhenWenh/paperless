package at.fhtw.swen.paperless.api.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class TagDto {

    private UUID id;
    private String name;
}
