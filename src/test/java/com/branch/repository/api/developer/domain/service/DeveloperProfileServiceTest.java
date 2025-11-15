package com.branch.repository.api.developer.domain.service;

import com.branch.repository.api.developer.application.port.out.LoadDeveloperProfilePort;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import com.branch.repository.api.developer.domain.model.DeveloperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit test for DeveloperProfileService.
 * Tests the domain service logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class DeveloperProfileServiceTest {

    @Mock
    private LoadDeveloperProfilePort loadDeveloperProfilePort;

    private DeveloperProfileService service;

    @BeforeEach
    void setUp() {
        service = new DeveloperProfileService(loadDeveloperProfilePort);
    }

    @Test
    void getDeveloperProfile_shouldReturnProfileWithUserInfo() {
        // Given
        DeveloperProfile expectedProfile = new DeveloperProfile(
            "octocat",
            "The Octocat",
            "https://avatars.githubusercontent.com/u/583231?v=4",
            "San Francisco",
            null,
            "https://api.github.com/users/octocat",
            ZonedDateTime.parse("2011-01-25T18:44:36Z"),
            List.of()
        );
        when(loadDeveloperProfilePort.getDeveloperProfile("octocat")).thenReturn(expectedProfile);

        // When
        DeveloperProfile profile = service.getDeveloperProfile("octocat");

        // Then
        assertNotNull(profile);
        assertEquals("octocat", profile.userName());
        assertEquals("The Octocat", profile.displayName());
        assertEquals("https://avatars.githubusercontent.com/u/583231?v=4", profile.avatar());
        assertEquals("San Francisco", profile.geoLocation());
        assertNull(profile.email());
        assertEquals("https://api.github.com/users/octocat", profile.url());
        assertNotNull(profile.createdAt());
    }

    @Test
    void getDeveloperProfile_shouldReturnProfileWithRepositories() {
        // Given
        DeveloperProfile expectedProfile = new DeveloperProfile(
            "octocat",
            "The Octocat",
            "https://avatars.githubusercontent.com/u/583231?v=4",
            "San Francisco",
            null,
            "https://api.github.com/users/octocat",
            ZonedDateTime.parse("2011-01-25T18:44:36Z"),
            List.of(
                new DeveloperRepository("boysenberry-repo-1", "https://api.github.com/repos/octocat/boysenberry-repo-1"),
                new DeveloperRepository("git-consortium", "https://api.github.com/repos/octocat/git-consortium"),
                new DeveloperRepository("hello-worId", "https://api.github.com/repos/octocat/hello-worId")
            )
        );
        when(loadDeveloperProfilePort.getDeveloperProfile("octocat")).thenReturn(expectedProfile);

        // When
        DeveloperProfile profile = service.getDeveloperProfile("octocat");

        // Then
        assertNotNull(profile.repos());
        assertEquals(3, profile.getRepoCount());
        assertEquals("boysenberry-repo-1", profile.repos().get(0).name());
    }
}