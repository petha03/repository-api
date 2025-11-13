package com.branch.repository.api.developer.adapter.out.github.model;

/**
 * DTO for GitHub Repository API response.
 * Maps to the response from GET /users/{username}/repos
 */
public record GitHubRepositoryResponse(
    String name,
    String url
) {
}