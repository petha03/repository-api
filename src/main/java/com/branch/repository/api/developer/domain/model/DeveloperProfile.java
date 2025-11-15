package com.branch.repository.api.developer.domain.model;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Domain model representing a developer's profile with their repositories.
 * This is the core business entity in the hexagonal architecture.
 */
public record DeveloperProfile(
    String userName,
    String displayName,
    String avatar,
    String geoLocation,
    String email,
    String url,
    ZonedDateTime createdAt,
    List<DeveloperRepository> repos
) {
    public int getRepoCount() {
        return repos != null ? repos.size() : 0;
    }
}