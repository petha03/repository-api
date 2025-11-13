package com.branch.repository.api.infrastructure.rest;

/**
 * Exception thrown for HTTP errors that should be retried.
 * This includes:
 * - 5xx server errors (500-599)
 * - 429 Too Many Requests (rate limiting)
 * - Network/connection errors
 */
public class RetryableHttpException extends RuntimeException {

    private final int statusCode;

    public RetryableHttpException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RetryableHttpException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Determines if an HTTP status code should trigger a retry.
     *
     * @param statusCode the HTTP status code
     * @return true if the request should be retried
     */
    public static boolean isRetryable(int statusCode) {
        // Retry on server errors (5xx) or rate limiting (429)
        return statusCode >= 500 || statusCode == 429;
    }
}