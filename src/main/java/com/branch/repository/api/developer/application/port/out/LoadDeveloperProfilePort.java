package com.branch.repository.api.developer.application.port.out;

import com.branch.repository.api.developer.domain.model.DeveloperProfile;

/**
 * Output port (driven port) for loading developer profile data.
 * This will be implemented by an adapter that fetches data from GitHub API.
 */
public interface LoadDeveloperProfilePort {
    /**
     * Loads a developer's profile from an external source (e.g., GitHub API).
     *
     * @param username the GitHub username
     * @return the developer's profile
     */
    DeveloperProfile getDeveloperProfile(String username);
}