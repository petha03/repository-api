package com.branch.repository.api.infrastructure.exception;

import com.branch.repository.api.infrastructure.rest.RetryableHttpException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for GlobalExceptionHandler.
 * Tests exception handling and RFC 7807 Problem Detail response generation.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/test");
    }

    @Test
    void handleConstraintViolation_shouldReturnBadRequestWithViolations() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();

        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("username");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("must not be blank");
        violations.add(violation1);

        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path2 = mock(Path.class);
        when(path2.toString()).thenReturn("email");
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("must be a valid email");
        violations.add(violation2);

        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // When
        ProblemDetail problemDetail = exceptionHandler.handleConstraintViolation(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals("Validation Error", problemDetail.getTitle());
        assertEquals("Validation failed", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
        assertTrue(problemDetail.getProperties().get("timestamp") instanceof ZonedDateTime);

        @SuppressWarnings("unchecked")
        List<String> violationMessages = (List<String>) problemDetail.getProperties().get("violations");
        assertNotNull(violationMessages);
        assertEquals(2, violationMessages.size());
        assertTrue(violationMessages.stream().anyMatch(msg -> msg.contains("username")));
        assertTrue(violationMessages.stream().anyMatch(msg -> msg.contains("email")));
    }

    @Test
    void handleHttpClientError_shouldReturnClientErrorResponse() {
        // Given
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized access"
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleHttpClientError(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED.value(), problemDetail.getStatus());
        assertEquals("Client Error", problemDetail.getTitle());
        assertNotNull(problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleHttpClientError_shouldHandleNotFoundSpecially() {
        // Given
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.NOT_FOUND,
            "Not found"
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleHttpClientError(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Resource Not Found", problemDetail.getTitle());
        assertEquals("The requested resource was not found", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleHttpServerError_shouldReturnBadGateway() {
        // Given
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Server error"
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleHttpServerError(exception);

        // Then
        assertEquals(HttpStatus.BAD_GATEWAY.value(), problemDetail.getStatus());
        assertEquals("External Service Error", problemDetail.getTitle());
        assertEquals("External service error", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
        assertEquals(500, problemDetail.getProperties().get("originalStatus"));
    }

    @Test
    void handleRetryableHttpException_shouldReturnServiceUnavailable() {
        // Given
        RetryableHttpException exception = new RetryableHttpException(
            "Service unavailable after retries",
            503
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleRetryableHttpException(exception);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), problemDetail.getStatus());
        assertEquals("Service Unavailable", problemDetail.getTitle());
        assertEquals("Service temporarily unavailable after multiple retry attempts", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
        assertEquals(503, problemDetail.getProperties().get("originalStatus"));
    }

    @Test
    void handleRetryableHttpException_shouldHandleRateLimitExhaustion() {
        // Given
        RetryableHttpException exception = new RetryableHttpException(
            "Rate limit exceeded after retries",
            429
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleRetryableHttpException(exception);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), problemDetail.getStatus());
        assertEquals(429, problemDetail.getProperties().get("originalStatus"));
    }

    @Test
    void handleMissingPathVariable_shouldReturnNotFound() {
        // Given
        MissingPathVariableException exception = new MissingPathVariableException(
            "username",
            null
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleMissingPathVariable(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertEquals("Resource not found", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleNoResourceFound_shouldReturnNotFound() {
        // Given
        NoResourceFoundException exception = new NoResourceFoundException(null, "/api/developers/");

        // When
        ProblemDetail problemDetail = exceptionHandler.handleNoResourceFound(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertEquals("Resource not found", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid username format");

        // When
        ProblemDetail problemDetail = exceptionHandler.handleIllegalArgument(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals("Invalid Argument", problemDetail.getTitle());
        assertEquals("Invalid username format", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        // Given
        Exception exception = new RuntimeException("Internal error");

        // When
        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Internal Server Error", problemDetail.getTitle());
        assertEquals("An unexpected error occurred", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
        // Should not expose internal error message
        assertFalse(problemDetail.getDetail().contains("Internal error"));
    }

    @Test
    void allHandlers_shouldIncludeTimestamp() {
        // Given - various exceptions
        IllegalArgumentException illegalArgEx = new IllegalArgumentException("Test");
        RetryableHttpException retryableEx = new RetryableHttpException("Test", 500);
        HttpClientErrorException clientEx = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Test");

        // When
        ProblemDetail pd1 = exceptionHandler.handleIllegalArgument(illegalArgEx);
        ProblemDetail pd2 = exceptionHandler.handleRetryableHttpException(retryableEx);
        ProblemDetail pd3 = exceptionHandler.handleHttpClientError(clientEx);

        // Then - all should have timestamp
        assertNotNull(pd1.getProperties().get("timestamp"));
        assertNotNull(pd2.getProperties().get("timestamp"));
        assertNotNull(pd3.getProperties().get("timestamp"));
        assertTrue(pd1.getProperties().get("timestamp") instanceof ZonedDateTime);
        assertTrue(pd2.getProperties().get("timestamp") instanceof ZonedDateTime);
        assertTrue(pd3.getProperties().get("timestamp") instanceof ZonedDateTime);
    }

    @Test
    void handleHttpServerError_shouldPreserveOriginalStatusCode() {
        // Given - different 5xx errors
        HttpServerErrorException error502 = new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Gateway error");
        HttpServerErrorException error503 = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service down");

        // When
        ProblemDetail pd1 = exceptionHandler.handleHttpServerError(error502);
        ProblemDetail pd2 = exceptionHandler.handleHttpServerError(error503);

        // Then - should preserve original status in property
        assertEquals(502, pd1.getProperties().get("originalStatus"));
        assertEquals(503, pd2.getProperties().get("originalStatus"));
        // But return BAD_GATEWAY to client
        assertEquals(HttpStatus.BAD_GATEWAY.value(), pd1.getStatus());
        assertEquals(HttpStatus.BAD_GATEWAY.value(), pd2.getStatus());
    }

    @Test
    void handleConstraintViolation_shouldFormatViolationsCorrectly() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("user.email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be a well-formed email address");
        violations.add(violation);

        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // When
        ProblemDetail problemDetail = exceptionHandler.handleConstraintViolation(exception);

        // Then
        @SuppressWarnings("unchecked")
        List<String> violationMessages = (List<String>) problemDetail.getProperties().get("violations");
        assertEquals(1, violationMessages.size());
        assertEquals("user.email: must be a well-formed email address", violationMessages.get(0));
    }
}