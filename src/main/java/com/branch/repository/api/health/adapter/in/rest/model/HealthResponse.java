package com.branch.repository.api.health.adapter.in.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for the REST API response.
 * This is part of the adapter layer, translating domain models to API contracts.
 */
@Schema(description = "Health check response containing application status and timestamp")
public record HealthResponse(
    @Schema(description = "Application health status", example = "UP")
    String status,

    @Schema(description = "Timestamp when health check was performed (Unix epoch milliseconds)", example = "1763070888201")
    long timestamp
) {
}