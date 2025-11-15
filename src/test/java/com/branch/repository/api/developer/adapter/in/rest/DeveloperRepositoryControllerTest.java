package com.branch.repository.api.developer.adapter.in.rest;

import com.branch.repository.api.developer.adapter.in.rest.mapper.DeveloperProfileMapper;
import com.branch.repository.api.developer.adapter.in.rest.model.DeveloperProfileResponse;
import com.branch.repository.api.developer.adapter.in.rest.model.DeveloperRepositoryResponse;
import com.branch.repository.api.developer.application.port.in.QueryDeveloperProfileUseCase;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import com.branch.repository.api.developer.domain.model.DeveloperRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test for DeveloperRepositoryController.
 * Tests the REST adapter in isolation using mocked dependencies.
 */
@WebMvcTest(controllers = DeveloperRepositoryController.class,
    excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
@org.springframework.context.annotation.Import(com.branch.repository.api.infrastructure.jackson.JacksonConfig.class)
class DeveloperRepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryDeveloperProfileUseCase queryDeveloperProfileUseCase;

    @MockBean
    private DeveloperProfileMapper developerProfileMapper;

    @Test
    void getDeveloperProfile_shouldReturnDeveloperProfile() throws Exception {
        // Given
        DeveloperProfile profile = new DeveloperProfile(
            "octocat",
            "The Octocat",
            "https://avatars.githubusercontent.com/u/583231?v=4",
            "San Francisco",
            null,
            "https://api.github.com/users/octocat",
            ZonedDateTime.parse("2011-01-25T18:44:36Z"),
            List.of(
                new DeveloperRepository("boysenberry-repo-1", "https://api.github.com/repos/octocat/boysenberry-repo-1"),
                new DeveloperRepository("hello-world", "https://api.github.com/repos/octocat/hello-world")
            )
        );

        DeveloperProfileResponse response = new DeveloperProfileResponse(
            "octocat",
            "The Octocat",
            "https://avatars.githubusercontent.com/u/583231?v=4",
            "San Francisco",
            null,
            "https://api.github.com/users/octocat",
            ZonedDateTime.parse("2011-01-25T18:44:36Z"),
            List.of(
                new DeveloperRepositoryResponse("boysenberry-repo-1", "https://api.github.com/repos/octocat/boysenberry-repo-1"),
                new DeveloperRepositoryResponse("hello-world", "https://api.github.com/repos/octocat/hello-world")
            )
        );

        when(queryDeveloperProfileUseCase.getDeveloperProfile("octocat")).thenReturn(profile);
        when(developerProfileMapper.toDeveloperProfileResponse(any(DeveloperProfile.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/developers/octocat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("octocat"))
                .andExpect(jsonPath("$.display_name").value("The Octocat"))
                .andExpect(jsonPath("$.avatar").value("https://avatars.githubusercontent.com/u/583231?v=4"))
                .andExpect(jsonPath("$.geo_location").value("San Francisco"))
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.url").value("https://api.github.com/users/octocat"))
                .andExpect(jsonPath("$.created_at").value("Tue, 25 Jan 2011 18:44:36 GMT"))
                .andExpect(jsonPath("$.repos").isArray())
                .andExpect(jsonPath("$.repos[0].name").value("boysenberry-repo-1"))
                .andExpect(jsonPath("$.repos[0].url").value("https://api.github.com/repos/octocat/boysenberry-repo-1"));
    }
}
