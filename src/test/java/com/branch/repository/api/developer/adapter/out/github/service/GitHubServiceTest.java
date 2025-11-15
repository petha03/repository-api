package com.branch.repository.api.developer.adapter.out.github.service;

import com.branch.repository.api.common.BaseResourceTest;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubRepositoryResponse;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit test for GitHubService.
 * Tests RestTemplate integration and JSON parsing using MockRestServiceServer.
 */
class GitHubServiceTest extends BaseResourceTest {

    private GitHubService gitHubService;
    private MockRestServiceServer mockServer;
    private RestTemplate restTemplate;

    private static final String BASE_URL = "https://api.github.com";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        gitHubService = new GitHubService(restTemplate, BASE_URL);
    }

    @Test
    void getUserProfile_shouldCorrectlyParseCreatedAtField() throws Exception {
        // Given
        String username = "octocat";
        String jsonResponse = loadResourceAsString("github-user-response.json");

        mockServer.expect(requestTo(BASE_URL + "/users/" + username))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // When
        GitHubUserResponse response = gitHubService.getUserProfile(username);

        // Then
        assertNotNull(response);
        assertNotNull(response.createdAt(), "created_at field should be parsed");
        assertEquals(ZonedDateTime.parse("2011-01-25T18:44:36Z"), response.createdAt());
        assertEquals("octocat", response.login());
        assertEquals("The Octocat", response.name());

        mockServer.verify();
    }

    @Test
    void getUserProfile_shouldParseAllFields() throws Exception {
        // Given
        String username = "octocat";
        String jsonResponse = loadResourceAsString("github-user-response.json");

        mockServer.expect(requestTo(BASE_URL + "/users/" + username))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // When
        GitHubUserResponse response = gitHubService.getUserProfile(username);

        // Then
        assertNotNull(response);
        assertEquals("octocat", response.login());
        assertEquals("The Octocat", response.name());
        assertEquals("https://avatars.githubusercontent.com/u/583231?v=4", response.avatarUrl());
        assertEquals("San Francisco", response.location());
        assertNull(response.email());
        assertEquals("https://api.github.com/users/octocat", response.url());
        assertEquals(ZonedDateTime.parse("2011-01-25T18:44:36Z"), response.createdAt());

        mockServer.verify();
    }

    @Test
    void getUserProfile_shouldParseEmailWhenPresent() throws Exception {
        // Given
        String username = "testuser";
        String jsonResponse = loadResourceAsString("github-user-with-email-response.json");

        mockServer.expect(requestTo(BASE_URL + "/users/" + username))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // When
        GitHubUserResponse response = gitHubService.getUserProfile(username);

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.login());
        assertEquals("testuser@example.com", response.email());
        assertEquals(ZonedDateTime.parse("2020-06-15T10:30:00Z"), response.createdAt());

        mockServer.verify();
    }

    @Test
    void getUserRepositories_shouldParseRepositories() throws Exception {
        // Given
        String username = "octocat";
        String jsonResponse = loadResourceAsString("github-repositories-response.json");

        mockServer.expect(requestTo(BASE_URL + "/users/" + username + "/repos"))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // When
        GitHubRepositoryResponse[] response = gitHubService.getUserRepositories(username);

        // Then
        assertNotNull(response);
        assertEquals(3, response.length);
        assertEquals("boysenberry-repo-1", response[0].name());
        assertEquals("https://api.github.com/repos/octocat/boysenberry-repo-1", response[0].url());
        assertEquals("git-consortium", response[1].name());
        assertEquals("hello-worId", response[2].name());

        mockServer.verify();
    }

    @Test
    void getUserProfile_shouldHandleSnakeCaseToJsonProperty() throws Exception {
        // Given - Testing that snake_case fields (avatar_url, created_at) are correctly mapped to camelCase
        String username = "octocat";
        String jsonWithSnakeCase = """
            {
              "login": "octocat",
              "name": "The Octocat",
              "avatar_url": "https://avatars.githubusercontent.com/u/583231?v=4",
              "location": "San Francisco",
              "email": null,
              "url": "https://api.github.com/users/octocat",
              "created_at": "2011-01-25T18:44:36Z"
            }
            """;

        mockServer.expect(requestTo(BASE_URL + "/users/" + username))
            .andRespond(withSuccess(jsonWithSnakeCase, MediaType.APPLICATION_JSON));

        // When
        GitHubUserResponse response = gitHubService.getUserProfile(username);

        // Then - Verify @JsonProperty mapping works
        assertNotNull(response);
        assertEquals("https://avatars.githubusercontent.com/u/583231?v=4", response.avatarUrl(),
            "avatar_url should be mapped to avatarUrl");
        assertEquals(ZonedDateTime.parse("2011-01-25T18:44:36Z"), response.createdAt(),
            "created_at should be mapped to createdAt and parsed as ZonedDateTime");

        mockServer.verify();
    }

    /**
     * Helper method to load resource as String for MockRestServiceServer.
     */
    private String loadResourceAsString(String resourceName) throws Exception {
        try (var is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Resource not found: " + resourceName);
            return new String(is.readAllBytes());
        }
    }
}