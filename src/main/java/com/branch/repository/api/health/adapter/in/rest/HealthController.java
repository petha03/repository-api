package com.branch.repository.api.health.adapter.in.rest;

import com.branch.repository.api.health.adapter.in.rest.model.HealthResponse;
import com.branch.repository.api.health.application.port.in.HealthCheckUseCase;
import com.branch.repository.api.health.domain.model.HealthStatus;
import com.branch.repository.api.infrastructure.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Input adapter (driving adapter) for the health check feature.
 * This is the entry point from the outside world into the hexagon.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "API for checking application health status")
public class HealthController {

    private final HealthCheckUseCase healthCheckUseCase;

    public HealthController(HealthCheckUseCase healthCheckUseCase) {
        this.healthCheckUseCase = healthCheckUseCase;
    }

    @Operation(
        summary = "Check application health",
        description = "Returns the current health status of the application including timestamp"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application is healthy",
            content = @Content(schema = @Schema(implementation = HealthResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many requests - rate limit exceeded"
        )
    })
    @GetMapping
    @RateLimit(
        capacity = "${rate-limit.health.capacity}",
        refillTokens = "${rate-limit.health.refillTokens}",
        refillPeriod = "${rate-limit.health.refillPeriod}",
        refillUnit = "${rate-limit.health.refillUnit}"
    )
    public ResponseEntity<HealthResponse> checkHealth() {
        HealthStatus healthStatus = healthCheckUseCase.checkHealth();
        HealthResponse response = new HealthResponse(
            healthStatus.getStatus(),
            healthStatus.getTimestamp()
        );
        return ResponseEntity.ok(response);
    }
}