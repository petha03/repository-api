package com.branch.repository.api.infrastructure.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for GitHubUsernameValidator.
 * Tests various valid and invalid GitHub username formats.
 */
class GitHubUsernameValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "octocat",
        "github",
        "my-username",
        "user123",
        "a",
        "a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s",  // 39 chars
        "user-name-123"
    })
    void testValidGitHubUsernames(String username) {
        TestObject obj = new TestObject(username);
        Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);
        assertTrue(violations.isEmpty(), "Username should be valid: " + username);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-username",              // starts with hyphen
        "username-",              // ends with hyphen
        "user--name",             // consecutive hyphens
        "user name",              // contains space
        "user@name",              // invalid character
        "user.name",              // invalid character
        "user_name",              // invalid character
        "",                       // empty string
        "abcdefghijabcdefghijabcdefghijabcdefghij"  // 40 chars (too long)
    })
    void testInvalidGitHubUsernames(String username) {
        TestObject obj = new TestObject(username);
        Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);
        assertEquals(1, violations.size(), "Username should be invalid: " + username);
    }

    @Test
    void testNullUsername() {
        TestObject obj = new TestObject(null);
        Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);
        assertTrue(violations.isEmpty(), "Null username should be valid (handled by @NotNull if needed)");
    }

    // Test object to validate
    private static class TestObject {
        @ValidGitHubUsername
        private final String username;

        TestObject(String username) {
            this.username = username;
        }
    }
}