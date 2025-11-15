package com.branch.repository.api.infrastructure.exception;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for CustomErrorController.
 * Tests error handling for requests that cannot be mapped to any controller endpoint.
 */
class CustomErrorControllerTest {

    private CustomErrorController errorController;

    @BeforeEach
    void setUp() {
        errorController = new CustomErrorController();
    }

    @Test
    void handleError_shouldReturn404ForNotFound() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/nonexistent");

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertEquals("The requested resource was not found: /api/nonexistent", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
        assertTrue(problemDetail.getProperties().get("timestamp") instanceof ZonedDateTime);
    }

    @Test
    void handleError_shouldReturn404WithoutUriWhenNotProvided() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("The requested resource was not found", problemDetail.getDetail());
    }

    @Test
    void handleError_shouldReturn405ForMethodNotAllowed() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 405);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/developers/octocat");

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), problemDetail.getStatus());
        assertEquals("Method Not Allowed", problemDetail.getTitle());
        assertEquals("HTTP method not allowed for this endpoint", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleError_shouldReturn415ForUnsupportedMediaType() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 415);

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), problemDetail.getStatus());
        assertEquals("Unsupported Media Type", problemDetail.getTitle());
        assertEquals("Unsupported media type", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleError_shouldReturn406ForNotAcceptable() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 406);

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), problemDetail.getStatus());
        assertEquals("Not Acceptable", problemDetail.getTitle());
        assertEquals("Requested media type not acceptable", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleError_shouldReturn500WhenStatusCodeIsNull() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No status code set

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Internal Server Error", problemDetail.getTitle());
        assertEquals("An error occurred", problemDetail.getDetail());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleError_shouldReturn500ForInvalidStatusCode() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 999); // Invalid status code

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Internal Server Error", problemDetail.getTitle());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleError_shouldUseErrorMessageWhenProvided() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Database connection failed");

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Database connection failed", problemDetail.getDetail());
    }

    @Test
    void handleError_shouldUseDefaultMessageWhenErrorMessageIsNull() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 503);

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), problemDetail.getStatus());
        assertEquals("An error occurred", problemDetail.getDetail());
    }

    @Test
    void handleError_shouldIncludeTimestampInGMT() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        // When
        ProblemDetail problemDetail = errorController.handleError(request);

        // Then
        Object timestamp = problemDetail.getProperties().get("timestamp");
        assertNotNull(timestamp);
        assertTrue(timestamp instanceof ZonedDateTime);
        ZonedDateTime zonedTimestamp = (ZonedDateTime) timestamp;
        assertEquals("GMT", zonedTimestamp.getZone().getId());
    }

    @Test
    void handleError_shouldHandleMultipleStatusCodes() {
        // Test various status codes
        int[] statusCodes = {400, 401, 403, 404, 500, 502, 503};

        for (int statusCode : statusCodes) {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, statusCode);

            // When
            ProblemDetail problemDetail = errorController.handleError(request);

            // Then
            assertEquals(statusCode, problemDetail.getStatus(),
                    "Status code should match for " + statusCode);
            assertNotNull(problemDetail.getTitle(),
                    "Title should not be null for status " + statusCode);
            assertNotNull(problemDetail.getDetail(),
                    "Detail should not be null for status " + statusCode);
            assertNotNull(problemDetail.getProperties().get("timestamp"),
                    "Timestamp should not be null for status " + statusCode);
        }
    }
}