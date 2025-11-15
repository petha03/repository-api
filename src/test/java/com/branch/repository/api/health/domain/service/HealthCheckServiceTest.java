package com.branch.repository.api.health.domain.service;

import com.branch.repository.api.health.domain.model.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for HealthCheckService.
 * Tests the domain service logic in isolation.
 */
class HealthCheckServiceTest {

    private HealthCheckService healthCheckService;

    @BeforeEach
    void setUp() {
        healthCheckService = new HealthCheckService();
    }

    @Test
    void checkHealth_shouldReturnUpStatus() {
        // When
        HealthStatus healthStatus = healthCheckService.checkHealth();

        // Then
        assertNotNull(healthStatus);
        assertEquals("UP", healthStatus.getStatus());
        assertTrue(healthStatus.isHealthy());
    }

    @Test
    void checkHealth_shouldReturnCurrentTimestamp() {
        // Given
        long beforeCall = System.currentTimeMillis();

        // When
        HealthStatus healthStatus = healthCheckService.checkHealth();

        // Then
        long afterCall = System.currentTimeMillis();
        assertTrue(healthStatus.getTimestamp() >= beforeCall);
        assertTrue(healthStatus.getTimestamp() <= afterCall);
    }
}