package at.fhtw.swen.paperless.api.controller;

import at.fhtw.swen.paperless.api.controller.response.HealthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public HealthResponse getHealth() {
        log.debug("Health check requested");
        return new HealthResponse("UP", Instant.now());
    }
}
