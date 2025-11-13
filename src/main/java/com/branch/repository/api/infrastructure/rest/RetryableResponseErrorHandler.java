package com.branch.repository.api.infrastructure.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

/**
 * Custom error handler for RestTemplate that converts retryable HTTP errors
 * into RetryableHttpException for automatic retry processing.
 *
 * This centralizes retry logic so individual service methods don't need
 * try-catch blocks to handle retryable errors.
 */
public class RetryableResponseErrorHandler extends DefaultResponseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(RetryableResponseErrorHandler.class);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = (HttpStatus) response.getStatusCode();
        int statusValue = statusCode.value();

        // If this is a retryable error, throw RetryableHttpException
        if (RetryableHttpException.isRetryable(statusValue)) {
            log.debug("Detected retryable HTTP status: {}. Converting to RetryableHttpException", statusValue);
            throw new RetryableHttpException(
                "HTTP request failed with retryable status: " + statusValue,
                statusValue
            );
        }

        // Otherwise, use default error handling (throws HttpClientErrorException, HttpServerErrorException, etc.)
        log.debug("Non-retryable HTTP status: {}. Using default error handling", statusValue);
        super.handleError(response);
    }
}