package com.branch.repository.api.developer.application.port.in;

import com.branch.repository.api.developer.domain.model.DeveloperProfile;

public interface QueryDeveloperProfileUseCase {
    /**
     * Retrieves a developer's profile by username.
     *
     * @param username the GitHub username
     * @return the developer's profile
     */
    DeveloperProfile getDeveloperProfile(String username);
}