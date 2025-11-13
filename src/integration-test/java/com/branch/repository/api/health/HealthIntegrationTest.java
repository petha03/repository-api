package com.branch.repository.api.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the health check feature.
 * Tests the entire feature end-to-end with full Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_shouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void healthEndpoint_shouldReturn429WhenRateLimitExceeded() throws Exception {
        // Given - rate limit is 1 request per 5 minutes
        // When - make first request (should succeed)
        mockMvc.perform(get("/api/health")
                        .header("X-Forwarded-For", "203.0.113.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // When - make second request immediately (should be rate limited)
        mockMvc.perform(get("/api/health")
                        .header("X-Forwarded-For", "203.0.113.100"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Rate Limit Exceeded"))
                .andExpect(jsonPath("$.detail").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}