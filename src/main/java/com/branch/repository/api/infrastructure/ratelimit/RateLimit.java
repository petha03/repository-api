package com.branch.repository.api.infrastructure.ratelimit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for applying rate limiting to methods or endpoints.
 * Uses a token bucket algorithm to control the rate of requests.
 *
 * <p>Supports SpEL expressions for configuration values:
 * <pre>
 * {@code
 * @RateLimit(
 *     capacity = "${rate-limit.health.capacity}",
 *     refillTokens = "${rate-limit.health.refillTokens}",
 *     refillPeriod = "${rate-limit.health.refillPeriod}",
 *     refillUnit = "${rate-limit.health.refillUnit}"
 * )
 * public void myMethod() {
 *     // method implementation
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * The maximum number of tokens the bucket can hold.
     * This represents the maximum burst capacity.
     * Supports SpEL expressions like "${rate-limit.capacity}".
     *
     * @return the bucket capacity or SpEL expression
     */
    String capacity();

    /**
     * The number of tokens to add to the bucket during each refill period.
     * Supports SpEL expressions like "${rate-limit.refillTokens}".
     *
     * @return the number of tokens to refill or SpEL expression
     */
    String refillTokens();

    /**
     * The time period value for refilling tokens.
     * Used in combination with {@link #refillUnit()}.
     * Supports SpEL expressions like "${rate-limit.refillPeriod}".
     *
     * @return the refill period value or SpEL expression
     */
    String refillPeriod();

    /**
     * The time unit for the refill period.
     * Works with {@link #refillPeriod()} to define the refill rate.
     * Supports SpEL expressions like "${rate-limit.refillUnit}".
     * Valid values: NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS
     *
     * @return the time unit for refill period or SpEL expression
     */
    String refillUnit() default "SECONDS";
}