package com.branch.repository.api.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator implementation for GitHub username format.
 * Validates that usernames follow GitHub's username rules:
 * - 1-39 characters
 * - Alphanumeric or hyphens only
 * - Cannot start or end with hyphen
 * - Cannot contain consecutive hyphens
 */
public class GitHubUsernameValidator implements ConstraintValidator<ValidGitHubUsername, String> {

    // GitHub username pattern: alphanumeric and hyphens, 1-39 chars, no leading/trailing/consecutive hyphens
    private static final Pattern GITHUB_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$");

    @Override
    public void initialize(ValidGitHubUsername constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull annotation if needed
        if (username == null) {
            return true;
        }

        // Validate against GitHub username pattern
        if (!GITHUB_USERNAME_PATTERN.matcher(username).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "GitHub username must be 1-39 characters, contain only alphanumeric characters or hyphens, " +
                "and cannot start/end with a hyphen or contain consecutive hyphens"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}