package com.branch.repository.api.developer.adapter.out.github.service;

import com.branch.repository.api.developer.adapter.out.github.model.GitHubRepositoryResponse;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubUserResponse;
import com.branch.repository.api.infrastructure.cache.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for interacting with the GitHub API.
 * Handles HTTP communication for GitHub-specific operations.
 * Retry logic with exponential backoff is automatically applied via BeanNameAutoProxyCreator.
 * Retryable HTTP errors are automatically handled by RetryableResponseErrorHandler.
 * Responses are cached to reduce API calls and improve performance.
 */
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public GitHubService(
            @Qualifier("githubRestTemplate") RestTemplate restTemplate,
            @Value("${github.api.base-url:https://api.github.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        log.info("GitHubService initialized with base URL: {}", baseUrl);
    }

    /**
     * Fetches user profile information from GitHub API.
     * Automatically retries on server errors (5xx) and rate limiting (429) with exponential backoff.
     * Error handling is delegated to RetryableResponseErrorHandler.
     * Results are cached to reduce API calls.
     *
     * @param username the GitHub username
     * @return the user profile response
     */
    @Cacheable(value = CacheConfig.GITHUB_USER_PROFILE_CACHE, keyGenerator = "gitHubCacheKeyGenerator")
    public GitHubUserResponse getUserProfile(String username) {
        String url = "%s/users/{username}".formatted(baseUrl);
        log.debug("Fetching user profile for username: {} (cache miss)", username);

        try {
            GitHubUserResponse response = restTemplate.getForObject(url, GitHubUserResponse.class, username);
            log.info("Successfully fetched user profile for username: {} from GitHub API", username);
            if (response != null) {
                log.debug("User profile response: login={}, name={}", response.login(), response.name());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch user profile for username: {}. Error: {}", username, e.getMessage());
            throw e;
        }
    }

    /**
     * Fetches all repositories for a given GitHub user.
     * Automatically retries on server errors (5xx) and rate limiting (429) with exponential backoff.
     * Error handling is delegated to RetryableResponseErrorHandler.
     * Results are cached to reduce API calls.
     *
     * @param username the GitHub username
     * @return array of repository responses
     */
    @Cacheable(value = CacheConfig.GITHUB_USER_REPOSITORIES_CACHE, keyGenerator = "gitHubCacheKeyGenerator")
    public GitHubRepositoryResponse[] getUserRepositories(String username) {
        String url = "%s/users/{username}/repos".formatted(baseUrl);
        log.debug("Fetching repositories for username: {} (cache miss)", username);

        try {
            GitHubRepositoryResponse[] response = restTemplate.getForObject(url, GitHubRepositoryResponse[].class, username);
            int repoCount = response != null ? response.length : 0;
            log.info("Successfully fetched {} repositories for username: {} from GitHub API", repoCount, username);
            if (log.isDebugEnabled() && response != null && response.length > 0) {
                log.debug("First repository: name={}, url={}", response[0].name(), response[0].url());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch repositories for username: {}. Error: {}", username, e.getMessage());
            throw e;
        }
    }
}