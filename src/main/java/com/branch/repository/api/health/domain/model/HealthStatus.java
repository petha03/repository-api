package com.branch.repository.api.health.domain.model;

/**
 * Domain model representing the health status of the application.
 * This is the core business entity in the hexagonal architecture.
 */
public class HealthStatus {
    private final String status;
    private final long timestamp;

    public HealthStatus(String status, long timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isHealthy() {
        return "UP".equals(status);
    }
}