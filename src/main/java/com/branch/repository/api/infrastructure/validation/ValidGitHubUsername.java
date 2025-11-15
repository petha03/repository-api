package com.branch.repository.api.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for GitHub usernames.
 * GitHub usernames must:
 * - Be 1-39 characters long
 * - Contain only alphanumeric characters or hyphens
 * - Cannot start or end with a hyphen
 * - Cannot have consecutive hyphens
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GitHubUsernameValidator.class)
@Documented
public @interface ValidGitHubUsername {

    String message() default "Invalid GitHub username format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}