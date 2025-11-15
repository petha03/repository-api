package com.branch.repository.api.developer.adapter.out.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

/**
 * DTO for GitHub User API response.
 * Maps to the response from GET /users/{username}
 */
public record GitHubUserResponse(
    String login,
    String name,
    @JsonProperty("avatar_url")
    String avatarUrl,
    String location,
    String email,
    String url,
    @JsonProperty("created_at")
    ZonedDateTime createdAt
) {
}