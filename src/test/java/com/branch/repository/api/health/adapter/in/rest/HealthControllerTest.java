package com.branch.repository.api.health.adapter.in.rest;

import com.branch.repository.api.health.application.port.in.HealthCheckUseCase;
import com.branch.repository.api.health.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test for HealthController.
 * Tests the REST adapter in isolation using mocked dependencies.
 */
@WebMvcTest(controllers = HealthController.class,
    excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthCheckUseCase healthCheckUseCase;

    @Test
    void checkHealth_shouldReturnHealthStatus() throws Exception {
        // Given
        HealthStatus healthStatus = new HealthStatus("UP", 1234567890L);
        when(healthCheckUseCase.checkHealth()).thenReturn(healthStatus);

        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").value(1234567890L));
    }
}