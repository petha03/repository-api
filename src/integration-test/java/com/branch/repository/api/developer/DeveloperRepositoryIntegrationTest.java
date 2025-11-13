package com.branch.repository.api.developer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the developer profile feature.
 * Tests the entire feature end-to-end with full Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeveloperRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDeveloperProfile_shouldReturnCompleteDeveloperProfile() throws Exception {
        // Integration test that calls real GitHub API
        mockMvc.perform(get("/api/developers/octocat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("octocat"))
                .andExpect(jsonPath("$.display_name").value("The Octocat"))
                .andExpect(jsonPath("$.avatar").exists())
                .andExpect(jsonPath("$.geo_location").value("San Francisco"))
                .andExpect(jsonPath("$.url").value("https://api.github.com/users/octocat"))
                .andExpect(jsonPath("$.created_at").value("Tue, 25 Jan 2011 18:44:36 GMT"))
                .andExpect(jsonPath("$.repos").isArray())
                .andExpect(jsonPath("$.repos").isNotEmpty());
    }

    @Test
    void getDeveloperProfile_shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/developers/user-definitely-does-not-exist-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void getDeveloperProfile_shouldReturn400WhenUsernameHasInvalidFormat() throws Exception {
        mockMvc.perform(get("/api/developers/invalid--username"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void getDeveloperProfile_shouldReturn400WhenUsernameTooLong() throws Exception {
        mockMvc.perform(get("/api/developers/this-username-is-way-too-long-for-github-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void getDeveloperProfile_shouldReturn404WhenUsernameIsEmpty() throws Exception {
        mockMvc.perform(get("/api/developers/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void errorResponses_shouldIncludeRfc1123TimestampInGmt() throws Exception {
        // Test that error responses include a properly formatted RFC 1123 timestamp in GMT
        mockMvc.perform(get("/api/developers/user-definitely-does-not-exist-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                // RFC 1123 format: "Tue, 3 Jun 2008 11:05:30 GMT"
                .andExpect(jsonPath("$.timestamp", matchesPattern(
                    "\\w{3}, \\d{1,2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT"
                )));
    }

    @Test
    void getDeveloperProfile_shouldReturn429WhenRateLimitExceeded() throws Exception {
        // Given - rate limit is 10 requests per minute per IP
        // Using a unique test IP to avoid interference with other tests
        String testIp = "203.0.113.201";

        // When - make requests up to the limit
        // Note: First request may call GitHub API, subsequent may be cached
        // We need to consume all 10 tokens from the rate limiter
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/developers/octocat")
                            .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk());
        }

        // When - make request exceeding the limit (should be rate limited)
        mockMvc.perform(get("/api/developers/octocat")
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Rate Limit Exceeded"))
                .andExpect(jsonPath("$.detail").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp", matchesPattern(
                    "\\w{3}, \\d{1,2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT"
                )));
    }
}