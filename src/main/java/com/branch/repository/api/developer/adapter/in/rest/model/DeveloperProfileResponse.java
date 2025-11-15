package com.branch.repository.api.developer.adapter.in.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * DTO for the REST API response.
 * This is part of the adapter layer, translating domain models to API contracts.
 */
@Schema(description = "GitHub developer profile with repositories")
public record DeveloperProfileResponse(
    // Required: GitHub always returns username (max 39 characters)
    @JsonProperty("user_name")
    @Schema(description = "GitHub username", example = "octocat", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is required")
    @Size(max = 39, message = "Username must not exceed 39 characters")
    String userName,

    // Optional: Can be null if user hasn't set a display name (max 255 characters)
    @JsonProperty("display_name")
    @Schema(description = "User's display name", example = "The Octocat", nullable = true)
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    String displayName,

    // Required: GitHub always returns avatar URL
    @Schema(description = "URL to user's avatar image", example = "https://avatars.githubusercontent.com/u/583231?v=4", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Avatar is required")
    @URL(message = "Avatar must be a valid URL")
    String avatar,

    // Optional: Can be null if user hasn't set location (max 255 characters)
    @JsonProperty("geo_location")
    @Schema(description = "User's geographic location", example = "San Francisco", nullable = true)
    @Size(max = 255, message = "Location must not exceed 255 characters")
    String geoLocation,

    // Optional: Can be null based on user privacy settings
    @Schema(description = "User's email address", example = "octocat@github.com", nullable = true)
    @Email(message = "Email must be valid")
    String email,

    // Required: GitHub always returns API URL for user
    @Schema(description = "GitHub API URL for the user", example = "https://api.github.com/users/octocat", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "URL is required")
    @URL(protocol = "https", message = "URL must be a valid HTTPS URL")
    String url,

    // Required: GitHub always returns creation timestamp
    // Jackson serializes ZonedDateTime using custom ZonedDateTimeSerializer
    @JsonProperty("created_at")
    @Schema(description = "Account creation timestamp (formatted as RFC 2822)", example = "Tue, 25 Jan 2011 18:44:36 GMT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Created timestamp is required")
    ZonedDateTime createdAt,

    // Required: Always return repositories list (can be empty, but not null)
    @Schema(description = "List of user's public repositories", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Repositories list is required")
    @Valid
    List<DeveloperRepositoryResponse> repos
) {
}