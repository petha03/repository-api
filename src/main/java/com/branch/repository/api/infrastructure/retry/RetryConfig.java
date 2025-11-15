package com.branch.repository.api.infrastructure.retry;

import com.branch.repository.api.infrastructure.rest.RetryableHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Spring Retry functionality.
 * Provides a RetryTemplate for handling transient failures with exponential backoff.
 */
@Configuration
public class RetryConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryConfig.class);

    @Value("${retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${retry.initial-interval:500}")
    private long initialInterval;

    @Value("${retry.multiplier:2.0}")
    private double multiplier;

    @Value("${retry.max-interval:5000}")
    private long maxInterval;

    /**
     * Creates a RetryTemplate configured for HTTP operations.
     * Retries on RetryableHttpException with exponential backoff.
     *
     * @return configured RetryTemplate
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RetryableHttpException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
            maxAttempts,
            retryableExceptions,
            true,  // traverseCauses
            false  // defaultValue (don't retry by default - only retry exceptions in the map)
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxInterval);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Add retry listeners for logging
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(
                RetryContext context,
                RetryCallback<T, E> callback
            ) {
                log.debug("Starting retry operation");
                return true;
            }

            @Override
            public <T, E extends Throwable> void close(
                RetryContext context,
                RetryCallback<T, E> callback,
                Throwable throwable
            ) {
                if (throwable != null) {
                    log.error("Retry exhausted after {} attempts. Final error: {}",
                        context.getRetryCount(),
                        throwable.getMessage());
                } else {
                    if (context.getRetryCount() > 0) {
                        log.info("Retry successful after {} attempts", context.getRetryCount());
                    }
                }
            }

            @Override
            public <T, E extends Throwable> void onError(
                RetryContext context,
                RetryCallback<T, E> callback,
                Throwable throwable
            ) {
                if (throwable instanceof RetryableHttpException retryableEx) {
                    log.warn("Retry attempt {} of {} failed with status {}: {}. Retrying...",
                        context.getRetryCount(),
                        maxAttempts,
                        retryableEx.getStatusCode(),
                        throwable.getMessage());
                } else {
                    log.warn("Retry attempt {} of {} failed: {}. Retrying...",
                        context.getRetryCount(),
                        maxAttempts,
                        throwable.getMessage());
                }
            }
        });

        log.info("RetryTemplate configured: maxAttempts={}, initialInterval={}ms, multiplier={}, maxInterval={}ms",
            maxAttempts, initialInterval, multiplier, maxInterval);

        return retryTemplate;
    }
}