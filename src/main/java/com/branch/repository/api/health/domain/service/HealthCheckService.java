package com.branch.repository.api.health.domain.service;

import com.branch.repository.api.health.application.port.in.HealthCheckUseCase;
import com.branch.repository.api.health.domain.model.HealthStatus;
import org.springframework.stereotype.Service;

/**
 * Domain service implementing the health check use case.
 * Contains the core business logic and implements the input port.
 */
@Service
public class HealthCheckService implements HealthCheckUseCase {

    @Override
    public HealthStatus checkHealth() {
        long currentTime = System.currentTimeMillis();
        return new HealthStatus("UP", currentTime);
    }
}