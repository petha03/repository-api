package com.branch.repository.api.developer.adapter.in.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * DTO for repository information in REST API responses.
 */
@Schema(description = "GitHub repository information")
public record DeveloperRepositoryResponse(
    // Required: GitHub always returns repository name (max 100 characters)
    @Schema(description = "Repository name", example = "Hello-World", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Repository name is required")
    @Size(max = 100, message = "Repository name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Repository name must contain only alphanumeric characters, dots, underscores, or hyphens")
    String name,

    // Required: GitHub always returns repository API URL
    @Schema(description = "GitHub API URL for the repository", example = "https://api.github.com/repos/octocat/Hello-World", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Repository URL is required")
    @URL(protocol = "https", message = "Repository URL must be a valid HTTPS URL")
    String url
) {
}