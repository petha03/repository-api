package com.branch.repository.api.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Bucket4jRateLimitingFilter.
 * Tests rate limiting behavior for slow limits and within limits.
 */
class Bucket4jRateLimitingFilterTest {

    private Bucket4jRateLimitingFilter filter;
    private RequestMappingHandlerMapping handlerMapping;
    private ObjectMapper objectMapper;
    private FilterChain filterChain;
    private Environment environment;

    @BeforeEach
    void setUp() {
        handlerMapping = mock(RequestMappingHandlerMapping.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        environment = mock(Environment.class);

        // Set up environment to resolve test rate limit properties
        when(environment.resolveRequiredPlaceholders("3")).thenReturn("3");
        when(environment.resolveRequiredPlaceholders("10")).thenReturn("10");
        when(environment.resolveRequiredPlaceholders("2")).thenReturn("2");
        when(environment.resolveRequiredPlaceholders("1")).thenReturn("1");
        when(environment.resolveRequiredPlaceholders("SECONDS")).thenReturn("SECONDS");

        filter = new Bucket4jRateLimitingFilter(handlerMapping, objectMapper, environment);
        filterChain = mock(FilterChain.class);
    }

    /**
     * Helper method to set up a mock handler with rate limit annotation
     */
    private void setupRateLimitedHandler(MockHttpServletRequest request, String methodName) throws Exception {
        Method method = TestController.class.getMethod(methodName);
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
        HandlerExecutionChain chain = new HandlerExecutionChain(handlerMethod);
        when(handlerMapping.getHandler(request)).thenReturn(chain);
    }

    /**
     * Helper method to create a request with IP address
     */
    private MockHttpServletRequest createRequest(String ipAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ipAddress);
        return request;
    }

    @Test
    void doFilter_withSlowLimit_shouldAllowRequestsWithinLimit() throws Exception {
        // Given - rate limit: 3 requests per 10 seconds
        MockHttpServletRequest request = createRequest("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupRateLimitedHandler(request, "slowLimitEndpoint");

        // When - make 3 requests (within limit)
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        // Then - all requests should pass through
        verify(filterChain, times(3)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_withSlowLimit_shouldBlockRequestsExceedingLimit() throws Exception {
        // Given - rate limit: 3 requests per 10 seconds
        MockHttpServletRequest request = createRequest("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupRateLimitedHandler(request, "slowLimitEndpoint");

        // When - make 4 requests (exceeding limit)
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);

        // Then - first 3 should pass, 4th should be blocked
        verify(filterChain, times(3)).doFilter(any(), any());
        assertEquals(429, blockedResponse.getStatus());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, blockedResponse.getContentType());
    }

    @Test
    void doFilter_withSlowLimit_shouldReturnProblemDetailWhenBlocked() throws Exception {
        // Given - rate limit: 3 requests per 10 seconds
        MockHttpServletRequest request = createRequest("192.168.1.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupRateLimitedHandler(request, "slowLimitEndpoint");

        // When - exceed rate limit
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);

        // Then - should return ProblemDetail
        String responseBody = blockedResponse.getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("Rate Limit Exceeded"));
        assertTrue(responseBody.contains("Too many requests"));
        assertTrue(responseBody.contains("\"status\":429"));
        assertTrue(responseBody.contains("timestamp"));
    }

    @Test
    void doFilter_withDifferentIPs_shouldHaveSeparateBuckets() throws Exception {
        // Given - rate limit: 3 requests per 10 seconds
        MockHttpServletRequest request1 = createRequest("192.168.1.1");
        MockHttpServletRequest request2 = createRequest("192.168.1.2");
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        when(handlerMapping.getHandler(any())).thenAnswer(invocation -> {
            Method method = TestController.class.getMethod("slowLimitEndpoint");
            HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
            return new HandlerExecutionChain(handlerMethod);
        });

        // When - each IP makes 3 requests
        filter.doFilter(request1, response1, filterChain);
        filter.doFilter(request1, response1, filterChain);
        filter.doFilter(request1, response1, filterChain);

        filter.doFilter(request2, response2, filterChain);
        filter.doFilter(request2, response2, filterChain);
        filter.doFilter(request2, response2, filterChain);

        // Then - all should pass (separate buckets per IP)
        verify(filterChain, times(6)).doFilter(any(), any());
    }

    @Test
    void doFilter_withXForwardedForHeader_shouldUseForwardedIP() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("10.0.0.1"); // proxy IP
        request.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.1"); // client IPs
        setupRateLimitedHandler(request, "slowLimitEndpoint");

        // When - make 3 requests with same X-Forwarded-For
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        // Then - should track by first IP in X-Forwarded-For (203.0.113.1)
        verify(filterChain, times(3)).doFilter(request, response);

        // Additional request should be blocked
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);
        assertEquals(429, blockedResponse.getStatus());
    }

    @Test
    void doFilter_withoutRateLimitAnnotation_shouldAlwaysAllow() throws Exception {
        // Given - method without @RateLimit annotation
        MockHttpServletRequest request = createRequest("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupRateLimitedHandler(request, "noLimitEndpoint");

        // When - make many requests
        for (int i = 0; i < 100; i++) {
            filter.doFilter(request, response, filterChain);
        }

        // Then - all should pass (no rate limit)
        verify(filterChain, times(100)).doFilter(request, response);
    }

    @Test
    void doFilter_withNullHandler_shouldAlwaysAllow() throws Exception {
        // Given - no handler found
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(handlerMapping.getHandler(request)).thenReturn(null);

        // When
        filter.doFilter(request, response, filterChain);

        // Then - should pass through
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withException_shouldAllowRequestThrough() throws Exception {
        // Given - handler mapping throws exception
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(handlerMapping.getHandler(request)).thenThrow(new RuntimeException("Test exception"));

        // When
        filter.doFilter(request, response, filterChain);

        // Then - should allow request through (graceful degradation)
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withFastLimit_shouldBlockQuickly() throws Exception {
        // Given - rate limit: 2 requests per 1 second
        MockHttpServletRequest request = createRequest("192.168.1.3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupRateLimitedHandler(request, "fastLimitEndpoint");

        // When - make 3 requests rapidly
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);

        // Then - third request should be blocked
        verify(filterChain, times(2)).doFilter(any(), any());
        assertEquals(429, blockedResponse.getStatus());
    }

    @Test
    void doFilter_problemDetail_shouldHaveCorrectStructure() throws Exception {
        // Given
        MockHttpServletRequest request = createRequest("192.168.1.4");
        setupRateLimitedHandler(request, "slowLimitEndpoint");

        // When - exceed limit
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);

        // Then - parse and validate ProblemDetail structure
        String json = blockedResponse.getContentAsString();
        assertFalse(json.isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Object> problemDetail = objectMapper.readValue(json, Map.class);

        assertEquals("Rate Limit Exceeded", problemDetail.get("title"));
        assertEquals(429, problemDetail.get("status"));
        assertEquals("Too many requests. Please try again later.", problemDetail.get("detail"));

        // Timestamp is in properties/extensions map
        assertTrue(json.contains("timestamp"), "Response should contain timestamp field");
    }

    @Test
    void doFilter_withFastLimit_shouldRefillTokensOverTime() throws Exception {
        // Given - rate limit: 2 requests per 1 second (refills 2 tokens every second)
        MockHttpServletRequest request = createRequest("192.168.1.5");
        setupRateLimitedHandler(request, "fastLimitEndpoint");

        // When - consume all tokens
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request, response1, filterChain);
        filter.doFilter(request, response2, filterChain);

        // Then - both should succeed
        assertEquals(200, response1.getStatus());
        assertEquals(200, response2.getStatus());
        verify(filterChain, times(2)).doFilter(any(), any());

        // When - immediately try another request
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);

        // Then - should be blocked (no tokens available)
        assertEquals(429, blockedResponse.getStatus());
        verify(filterChain, times(2)).doFilter(any(), any()); // still only 2 successful

        // When - wait for tokens to refill (1.1 seconds to be safe)
        Thread.sleep(1100);

        // Then - new requests should succeed (tokens refilled)
        MockHttpServletResponse response3 = new MockHttpServletResponse();
        MockHttpServletResponse response4 = new MockHttpServletResponse();
        filter.doFilter(request, response3, filterChain);
        filter.doFilter(request, response4, filterChain);

        assertEquals(200, response3.getStatus());
        assertEquals(200, response4.getStatus());
        verify(filterChain, times(4)).doFilter(any(), any()); // 2 more successful calls
    }

    @Test
    void doFilter_withSlowLimit_shouldRefillTokensGradually() throws Exception {
        // Given - rate limit: 3 requests per 10 seconds
        MockHttpServletRequest request = createRequest("192.168.1.6");
        setupRateLimitedHandler(request, "slowLimitEndpoint");

        // When - consume all 3 tokens
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        verify(filterChain, times(3)).doFilter(any(), any());

        // Then - 4th request should be blocked
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);
        assertEquals(429, blockedResponse.getStatus());

        // When - wait for partial refill (11 seconds allows all 3 tokens to refill)
        Thread.sleep(11000);

        // Then - should have refilled tokens and allow new requests
        MockHttpServletResponse response4 = new MockHttpServletResponse();
        filter.doFilter(request, response4, filterChain);
        assertEquals(200, response4.getStatus());
        verify(filterChain, times(4)).doFilter(any(), any()); // 1 more successful call
    }

    /**
     * Test controller with rate-limited endpoints
     */
    static class TestController {

        @RateLimit(capacity = "3", refillTokens = "3", refillPeriod = "10", refillUnit = "SECONDS")
        public void slowLimitEndpoint() {
            // Test endpoint with slow rate limit
        }

        @RateLimit(capacity = "2", refillTokens = "2", refillPeriod = "1", refillUnit = "SECONDS")
        public void fastLimitEndpoint() {
            // Test endpoint with fast rate limit
        }

        public void noLimitEndpoint() {
            // Test endpoint without rate limit
        }
    }
}