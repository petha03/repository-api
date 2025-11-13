package com.branch.repository.api.infrastructure.retry;

import com.branch.repository.api.infrastructure.rest.RetryableHttpException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RetryConfig.
 * Tests that the actual RetryTemplate bean is correctly configured.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "retry.max-attempts=3",
    "retry.initial-interval=50",
    "retry.multiplier=2.0",
    "retry.max-interval=200"
})
class RetryConfigTest {

    @Autowired
    private RetryTemplate retryTemplate;

    @Test
    void retryTemplate_shouldBeConfigured() {
        assertNotNull(retryTemplate, "RetryTemplate bean should be available");
    }

    @Test
    void retryTemplate_shouldRetryOnRetryableHttpException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);

        // When - First 2 attempts fail with RetryableHttpException, third succeeds
        String result = retryTemplate.execute(context -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RetryableHttpException("HTTP 500 error", 500);
            }
            return "success";
        });

        // Then
        assertEquals("success", result);
        assertEquals(3, attemptCount.get(), "Should have made 3 attempts (1 initial + 2 retries)");
    }

    @Test
    void retryTemplate_shouldRetryOn429RateLimitError() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);

        // When
        String result = retryTemplate.execute(context -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new RetryableHttpException("HTTP 429 Too Many Requests", 429);
            }
            return "success";
        });

        // Then
        assertEquals("success", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void retryTemplate_shouldRetryOn503ServiceUnavailable() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);

        // When
        String result = retryTemplate.execute(context -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new RetryableHttpException("HTTP 503 Service Unavailable", 503);
            }
            return "success";
        });

        // Then
        assertEquals("success", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void retryTemplate_shouldFailAfterMaxRetries() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);

        // When & Then
        RetryableHttpException exception = assertThrows(
            RetryableHttpException.class,
            () -> retryTemplate.execute(context -> {
                attemptCount.incrementAndGet();
                throw new RetryableHttpException("HTTP 500 error", 500);
            })
        );

        assertEquals(500, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("500"));
        assertEquals(3, attemptCount.get(), "Should have exhausted max attempts (3)");
    }

    @Test
    void retryTemplate_shouldNotRetryNonRetryableException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);

        // When & Then - RuntimeException is not configured for retry
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> retryTemplate.execute(context -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Non-retryable error");
            })
        );

        assertEquals("Non-retryable error", exception.getMessage());
        assertEquals(1, attemptCount.get(), "Should only attempt once for non-retryable exceptions");
    }

    @Test
    void retryTemplate_shouldRespectConfiguredMaxAttempts() {
        // Given - max-attempts=3 configured in @TestPropertySource
        AtomicInteger attemptCount = new AtomicInteger(0);

        // When & Then
        assertThrows(
            RetryableHttpException.class,
            () -> retryTemplate.execute(context -> {
                attemptCount.incrementAndGet();
                throw new RetryableHttpException("Always fails", 500);
            })
        );

        assertEquals(3, attemptCount.get(), "Should respect configured max-attempts");
    }

    @Test
    void retryTemplate_shouldApplyExponentialBackoff() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        long[] attemptTimes = new long[3];

        // When
        try {
            retryTemplate.execute(context -> {
                int attempt = attemptCount.getAndIncrement();
                attemptTimes[attempt] = System.currentTimeMillis();
                throw new RetryableHttpException("Retry test", 500);
            });
        } catch (RetryableHttpException e) {
            // Expected to fail after retries
        }

        // Then - Verify exponential backoff is applied
        long firstBackoff = attemptTimes[1] - attemptTimes[0];
        long secondBackoff = attemptTimes[2] - attemptTimes[1];

        // With initial=50ms and multiplier=2.0, expect:
        // First backoff: ~50ms
        // Second backoff: ~100ms
        assertTrue(firstBackoff >= 40, "First backoff should be at least 40ms, was: " + firstBackoff + "ms");
        assertTrue(secondBackoff > firstBackoff,
            String.format("Second backoff (%dms) should be greater than first (%dms) due to exponential multiplier",
                secondBackoff, firstBackoff));
    }

    @Test
    void retryableHttpException_isRetryable_shouldIdentifyRetryableStatusCodes() {
        // Server errors (5xx) should be retryable
        assertTrue(RetryableHttpException.isRetryable(500));
        assertTrue(RetryableHttpException.isRetryable(502));
        assertTrue(RetryableHttpException.isRetryable(503));
        assertTrue(RetryableHttpException.isRetryable(504));

        // Rate limiting (429) should be retryable
        assertTrue(RetryableHttpException.isRetryable(429));

        // Client errors (4xx except 429) should NOT be retryable
        assertFalse(RetryableHttpException.isRetryable(400));
        assertFalse(RetryableHttpException.isRetryable(401));
        assertFalse(RetryableHttpException.isRetryable(403));
        assertFalse(RetryableHttpException.isRetryable(404));

        // Success codes should NOT be retryable
        assertFalse(RetryableHttpException.isRetryable(200));
        assertFalse(RetryableHttpException.isRetryable(201));
    }
}