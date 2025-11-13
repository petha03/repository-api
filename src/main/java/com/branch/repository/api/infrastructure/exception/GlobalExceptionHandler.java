package com.branch.repository.api.infrastructure.exception;

import com.branch.repository.api.infrastructure.rest.RetryableHttpException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * Converts exceptions to RFC 7807 Problem Detail responses with GMT timestamps.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Returns the current timestamp in GMT timezone.
     *
     * @return ZonedDateTime in GMT
     */
    private ZonedDateTime gmtNow() {
        return ZonedDateTime.now(ZoneId.of("GMT"));
    }

    /**
     * Handles validation errors from Jakarta Bean Validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("timestamp", gmtNow());
        problemDetail.setProperty("violations", ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList())
        );
        return problemDetail;
    }

    /**
     * Handles 4xx HTTP client errors from external API calls.
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ProblemDetail handleHttpClientError(HttpClientErrorException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            status,
            ex.getMessage()
        );
        problemDetail.setTitle("Client Error");
        problemDetail.setProperty("timestamp", gmtNow());

        // Special handling for 404 Not Found
        if (status == HttpStatus.NOT_FOUND) {
            problemDetail.setTitle("Resource Not Found");
            problemDetail.setDetail("The requested resource was not found");
        }

        return problemDetail;
    }

    /**
     * Handles 5xx HTTP server errors from external API calls.
     * These should be rare as retryable errors are handled by retry mechanism.
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ProblemDetail handleHttpServerError(HttpServerErrorException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY,
            "External service error"
        );
        problemDetail.setTitle("External Service Error");
        problemDetail.setProperty("timestamp", gmtNow());
        problemDetail.setProperty("originalStatus", ex.getStatusCode().value());
        return problemDetail;
    }

    /**
     * Handles retryable HTTP exceptions that have exhausted all retry attempts.
     */
    @ExceptionHandler(RetryableHttpException.class)
    public ProblemDetail handleRetryableHttpException(RetryableHttpException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service temporarily unavailable after multiple retry attempts"
        );
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setProperty("timestamp", gmtNow());
        problemDetail.setProperty("originalStatus", ex.getStatusCode());
        return problemDetail;
    }

    /**
     * Handles missing path variable exceptions (e.g., /api/developers/).
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ProblemDetail handleMissingPathVariable(MissingPathVariableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "Resource not found"
        );
        problemDetail.setTitle("Not Found");
        problemDetail.setProperty("timestamp", gmtNow());
        problemDetail.setProperty("missingVariable", ex.getVariableName());
        return problemDetail;
    }

    /**
     * Handles no resource found exceptions (Spring 6.1+ for missing endpoints).
     * This can occur when a request partially matches a path pattern but is missing required path variables.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "Resource not found"
        );
        problemDetail.setTitle("Not Found");
        problemDetail.setProperty("timestamp", gmtNow());
        return problemDetail;
    }

    /**
     * Handles illegal argument exceptions (e.g., invalid input parameters).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setProperty("timestamp", gmtNow());
        return problemDetail;
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", gmtNow());
        // Don't expose internal error details in production
        return problemDetail;
    }
}