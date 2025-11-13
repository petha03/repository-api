package com.branch.repository.api.developer.adapter.out.github;

import com.branch.repository.api.developer.adapter.out.github.mapper.GitHubMapper;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubRepositoryResponse;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubUserResponse;
import com.branch.repository.api.developer.adapter.out.github.service.GitHubService;
import com.branch.repository.api.developer.application.port.out.LoadDeveloperProfilePort;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import org.springframework.stereotype.Component;

/**
 * REST adapter implementing the LoadDeveloperProfilePort output port.
 * Integrates with GitHub API to fetch developer profile and repository data.
 * Delegates API calls to GitHubService which handles RestTemplate and retry logic.
 * Uses MapStruct mapper for converting GitHub API responses to domain models.
 */
@Component
public class QueryDeveloperProfileAdapter implements LoadDeveloperProfilePort {

    private final GitHubService gitHubService;
    private final GitHubMapper gitHubMapper;

    public QueryDeveloperProfileAdapter(GitHubService gitHubService, GitHubMapper gitHubMapper) {
        this.gitHubService = gitHubService;
        this.gitHubMapper = gitHubMapper;
    }

    @Override
    public DeveloperProfile getDeveloperProfile(String username) {
        // Fetch user profile via GitHubService
        GitHubUserResponse userResponse = gitHubService.getUserProfile(username);

        // Fetch user repositories via GitHubService
        GitHubRepositoryResponse[] reposResponse = gitHubService.getUserRepositories(username);

        // Use MapStruct mapper to convert to domain model (mapper handles null array check)
        return gitHubMapper.toDeveloperProfile(userResponse, reposResponse);
    }
}