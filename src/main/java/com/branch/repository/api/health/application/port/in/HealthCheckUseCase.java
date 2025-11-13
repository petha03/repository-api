package com.branch.repository.api.health.application.port.in;

import com.branch.repository.api.health.domain.model.HealthStatus;

/**
 * Input port (driving port) for the health check use case.
 * This is called by the driving adapters (e.g., REST controllers).
 */
public interface HealthCheckUseCase {
    HealthStatus checkHealth();
}