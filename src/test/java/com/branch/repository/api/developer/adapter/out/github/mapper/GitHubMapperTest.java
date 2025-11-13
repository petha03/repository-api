package com.branch.repository.api.developer.adapter.out.github.mapper;

import com.branch.repository.api.common.BaseResourceTest;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubRepositoryResponse;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubUserResponse;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import com.branch.repository.api.developer.domain.model.DeveloperRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for GitHubMapper.
 * Tests MapStruct mappings using JSON resource files.
 */
class GitHubMapperTest extends BaseResourceTest {

    private GitHubMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(GitHubMapper.class);
    }

    @Test
    void toDeveloperProfile_shouldMapUserAndRepositories() throws IOException {
        // Given
        GitHubUserResponse userResponse = loadResource(
            "github-user-response.json",
            GitHubUserResponse.class
        );
        GitHubRepositoryResponse[] reposResponse = loadResource(
            "github-repositories-response.json",
            GitHubRepositoryResponse[].class
        );

        // When
        DeveloperProfile profile = mapper.toDeveloperProfile(userResponse, reposResponse);

        // Then
        assertNotNull(profile);
        assertEquals("octocat", profile.userName());
        assertEquals("The Octocat", profile.displayName());
        assertEquals("https://avatars.githubusercontent.com/u/583231?v=4", profile.avatar());
        assertEquals("San Francisco", profile.geoLocation());
        assertNull(profile.email());
        assertEquals("https://api.github.com/users/octocat", profile.url());
        assertEquals(ZonedDateTime.parse("2011-01-25T18:44:36Z"), profile.createdAt());

        assertNotNull(profile.repos());
        assertEquals(3, profile.repos().size());
        assertEquals(3, profile.getRepoCount());
    }

    @Test
    void toDeveloperProfile_shouldMapUserWithEmail() throws IOException {
        // Given
        GitHubUserResponse userResponse = loadResource(
            "github-user-with-email-response.json",
            GitHubUserResponse.class
        );
        GitHubRepositoryResponse[] reposResponse = new GitHubRepositoryResponse[0];

        // When
        DeveloperProfile profile = mapper.toDeveloperProfile(userResponse, reposResponse);

        // Then
        assertNotNull(profile);
        assertEquals("testuser", profile.userName());
        assertEquals("Test User", profile.displayName());
        assertEquals("testuser@example.com", profile.email());
        assertEquals("New York", profile.geoLocation());
        assertEquals(ZonedDateTime.parse("2020-06-15T10:30:00Z"), profile.createdAt());

        assertNotNull(profile.repos());
        assertEquals(0, profile.repos().size());
        assertEquals(0, profile.getRepoCount());
    }

    @Test
    void toDeveloperProfile_shouldHandleNullRepositoryArray() throws IOException {
        // Given
        GitHubUserResponse userResponse = loadResource(
            "github-user-response.json",
            GitHubUserResponse.class
        );

        // When
        DeveloperProfile profile = mapper.toDeveloperProfile(userResponse, (GitHubRepositoryResponse[]) null);

        // Then
        assertNotNull(profile);
        assertEquals("octocat", profile.userName());
        assertNotNull(profile.repos());
        assertEquals(0, profile.repos().size());
        assertEquals(0, profile.getRepoCount());
    }

    @Test
    void toDeveloperProfile_withList_shouldMapCorrectly() throws IOException {
        // Given
        GitHubUserResponse userResponse = loadResource(
            "github-user-response.json",
            GitHubUserResponse.class
        );
        List<GitHubRepositoryResponse> reposList = loadResource(
            "github-repositories-response.json",
            new TypeReference<List<GitHubRepositoryResponse>>() {}
        );

        // When
        DeveloperProfile profile = mapper.toDeveloperProfile(userResponse, reposList);

        // Then
        assertNotNull(profile);
        assertEquals("octocat", profile.userName());
        assertEquals(3, profile.repos().size());
    }

    @Test
    void toRepository_shouldMapSingleRepository() throws IOException {
        // Given
        GitHubRepositoryResponse[] repos = loadResource(
            "github-repositories-response.json",
            GitHubRepositoryResponse[].class
        );
        GitHubRepositoryResponse repoResponse = repos[0];

        // When
        DeveloperRepository repository = mapper.toRepository(repoResponse);

        // Then
        assertNotNull(repository);
        assertEquals("boysenberry-repo-1", repository.name());
        assertEquals("https://api.github.com/repos/octocat/boysenberry-repo-1", repository.url());
    }

    @Test
    void toRepositories_shouldMapRepositoryList() throws IOException {
        // Given
        List<GitHubRepositoryResponse> reposList = loadResource(
            "github-repositories-response.json",
            new TypeReference<List<GitHubRepositoryResponse>>() {}
        );

        // When
        List<DeveloperRepository> repositories = mapper.toRepositories(reposList);

        // Then
        assertNotNull(repositories);
        assertEquals(3, repositories.size());
        assertEquals("boysenberry-repo-1", repositories.get(0).name());
        assertEquals("git-consortium", repositories.get(1).name());
        assertEquals("hello-worId", repositories.get(2).name());
    }

    @Test
    void toDeveloperProfile_shouldMapAllRepositoryFields() throws IOException {
        // Given
        GitHubUserResponse userResponse = loadResource(
            "github-user-response.json",
            GitHubUserResponse.class
        );
        GitHubRepositoryResponse[] reposResponse = loadResource(
            "github-repositories-response.json",
            GitHubRepositoryResponse[].class
        );

        // When
        DeveloperProfile profile = mapper.toDeveloperProfile(userResponse, reposResponse);

        // Then
        List<DeveloperRepository> repos = profile.repos();
        assertEquals("boysenberry-repo-1", repos.get(0).name());
        assertEquals("https://api.github.com/repos/octocat/boysenberry-repo-1", repos.get(0).url());

        assertEquals("git-consortium", repos.get(1).name());
        assertEquals("https://api.github.com/repos/octocat/git-consortium", repos.get(1).url());

        assertEquals("hello-worId", repos.get(2).name());
        assertEquals("https://api.github.com/repos/octocat/hello-worId", repos.get(2).url());
    }
}