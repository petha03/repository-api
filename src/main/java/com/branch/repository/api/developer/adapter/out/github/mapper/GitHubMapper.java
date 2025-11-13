package com.branch.repository.api.developer.adapter.out.github.mapper;

import com.branch.repository.api.developer.adapter.out.github.model.GitHubRepositoryResponse;
import com.branch.repository.api.developer.adapter.out.github.model.GitHubUserResponse;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import com.branch.repository.api.developer.domain.model.DeveloperRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

/**
 * MapStruct mapper for converting GitHub API responses to domain models.
 * Spring component model ensures this is available as a Spring bean.
 */
@Mapper(componentModel = "spring")
public interface GitHubMapper {

    /**
     * Maps GitHubUserResponse and repository array to DeveloperProfile domain model.
     * Handles null array by converting to empty list.
     *
     * @param userResponse the GitHub user API response
     * @param repositoryResponses the GitHub repositories API response array (can be null)
     * @return DeveloperProfile domain model
     */
    default DeveloperProfile toDeveloperProfile(GitHubUserResponse userResponse, GitHubRepositoryResponse[] repositoryResponses) {
        List<GitHubRepositoryResponse> repositoryList = repositoryResponses != null
            ? Arrays.asList(repositoryResponses)
            : List.of();
        return toDeveloperProfile(userResponse, repositoryList);
    }

    /**
     * Maps GitHubUserResponse and repository list to DeveloperProfile domain model.
     *
     * @param userResponse the GitHub user API response
     * @param repositoryResponses the GitHub repositories API response list
     * @return DeveloperProfile domain model
     */
    @Mapping(source = "userResponse.login", target = "userName")
    @Mapping(source = "userResponse.name", target = "displayName")
    @Mapping(source = "userResponse.avatarUrl", target = "avatar")
    @Mapping(source = "userResponse.location", target = "geoLocation")
    @Mapping(source = "userResponse.email", target = "email")
    @Mapping(source = "userResponse.url", target = "url")
    @Mapping(source = "userResponse.createdAt", target = "createdAt")
    @Mapping(source = "repositoryResponses", target = "repos")
    DeveloperProfile toDeveloperProfile(GitHubUserResponse userResponse, List<GitHubRepositoryResponse> repositoryResponses);

    /**
     * Maps GitHubRepositoryResponse to Repository domain model.
     *
     * @param response the GitHub repository API response
     * @return Repository domain model
     */
    DeveloperRepository toRepository(GitHubRepositoryResponse response);

    /**
     * Maps list of GitHubRepositoryResponse to list of Repository domain models.
     *
     * @param responses list of GitHub repository API responses
     * @return list of Repository domain models
     */
    List<DeveloperRepository> toRepositories(List<GitHubRepositoryResponse> responses);
}