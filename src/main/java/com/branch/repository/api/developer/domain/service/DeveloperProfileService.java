package com.branch.repository.api.developer.domain.service;

import com.branch.repository.api.developer.application.port.in.QueryDeveloperProfileUseCase;
import com.branch.repository.api.developer.application.port.out.LoadDeveloperProfilePort;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import org.springframework.stereotype.Service;

/**
 * Domain service implementing the developer profile use case.
 * Contains the core business logic and implements the input port.
 * Delegates data fetching to the output port (implemented by adapters).
 */
@Service
public class DeveloperProfileService implements QueryDeveloperProfileUseCase {

    private final LoadDeveloperProfilePort loadDeveloperProfilePort;

    public DeveloperProfileService(LoadDeveloperProfilePort loadDeveloperProfilePort) {
        this.loadDeveloperProfilePort = loadDeveloperProfilePort;
    }

    @Override
    public DeveloperProfile getDeveloperProfile(String username) {
        // Delegate to output port - adapter will fetch from GitHub API
        return loadDeveloperProfilePort.getDeveloperProfile(username);
    }
}