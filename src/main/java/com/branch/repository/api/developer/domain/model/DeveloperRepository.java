package com.branch.repository.api.developer.domain.model;

/**
 * Domain model representing a GitHub repository.
 */
public record DeveloperRepository(
    String name,
    String url
) {
}