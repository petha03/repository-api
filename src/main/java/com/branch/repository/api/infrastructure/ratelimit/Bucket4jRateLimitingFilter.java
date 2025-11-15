package com.branch.repository.api.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that applies rate limiting based on the @RateLimit annotation.
 * Uses Bucket4j's token bucket algorithm to control request rates per IP address and method.
 *
 * <p>This filter runs with high priority (HIGHEST_PRECEDENCE + 1) to block rate-limited requests
 * early in the filter chain, before they consume application resources.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class Bucket4jRateLimitingFilter implements Filter {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    // each IP + method has its own bucket
    // Using Caffeine cache to prevent memory leak - evicts after 1 hour of inactivity
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(Duration.ofHours(1))
        .build();

    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public Bucket4jRateLimitingFilter(RequestMappingHandlerMapping handlerMapping, ObjectMapper objectMapper, Environment environment) {
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest) ||
            !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(httpRequest);

            if (handlerChain == null || !(handlerChain.getHandler() instanceof HandlerMethod handlerMethod)) {
                chain.doFilter(request, response);
                return;
            }

            RateLimit config = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RateLimit.class);
            if (config == null) {
                chain.doFilter(request, response);
                return;
            }

            String ip = getClientIp(httpRequest);
            String methodKey = handlerMethod.getMethod().toGenericString();
            String bucketKey = ip + ":" + methodKey;

            Bucket bucket = buckets.get(bucketKey, k -> createBucket(config));

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                sendRateLimitError(httpResponse);
            }

        } catch (Exception e) {
            // If there's any error in rate limiting logic, let the request through
            chain.doFilter(request, response);
        }
    }

    private Bucket createBucket(RateLimit cfg) {
        // Resolve property values from environment
        long capacity = resolvePropertyAsLong(cfg.capacity());
        long refillTokens = resolvePropertyAsLong(cfg.refillTokens());
        long refillPeriod = resolvePropertyAsLong(cfg.refillPeriod());
        TimeUnit refillUnit = resolvePropertyAsTimeUnit(cfg.refillUnit());

        ChronoUnit chronoUnit = convertToChronoUnit(refillUnit);
        Duration refillDuration = Duration.of(refillPeriod, chronoUnit);

        return Bucket.builder()
            .addLimit(limit -> limit
                .capacity(capacity)
                .refillGreedy(refillTokens, refillDuration))
            .build();
    }

    private long resolvePropertyAsLong(String value) {
        String resolved = environment.resolveRequiredPlaceholders(value);
        return Long.parseLong(resolved);
    }

    private TimeUnit resolvePropertyAsTimeUnit(String value) {
        String resolved = environment.resolveRequiredPlaceholders(value);
        return TimeUnit.valueOf(resolved);
    }

    private ChronoUnit convertToChronoUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS -> ChronoUnit.NANOS;
            case MICROSECONDS -> ChronoUnit.MICROS;
            case MILLISECONDS -> ChronoUnit.MILLIS;
            case SECONDS -> ChronoUnit.SECONDS;
            case MINUTES -> ChronoUnit.MINUTES;
            case HOURS -> ChronoUnit.HOURS;
            case DAYS -> ChronoUnit.DAYS;
        };
    }

    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Please try again later."
        );
        problemDetail.setTitle("Rate Limit Exceeded");
        problemDetail.setProperty("timestamp", ZonedDateTime.now(ZoneId.of("GMT")));

        response.setStatus(HTTP_TOO_MANY_REQUESTS);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}