package com.branch.repository.api.developer.adapter.in.rest;

import com.branch.repository.api.developer.adapter.in.rest.mapper.DeveloperProfileMapper;
import com.branch.repository.api.developer.adapter.in.rest.model.DeveloperProfileResponse;
import com.branch.repository.api.developer.application.port.in.QueryDeveloperProfileUseCase;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import com.branch.repository.api.infrastructure.ratelimit.RateLimit;
import com.branch.repository.api.infrastructure.validation.ValidGitHubUsername;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Input adapter (driving adapter) for the developer profile feature.
 * This is the entry point from the outside world into the hexagon.
 */
@RestController
@RequestMapping("/api/developers")
@Validated
@Tag(name = "Developer Profile", description = "APIs for retrieving GitHub developer profiles and repositories")
public class DeveloperRepositoryController {

    private final QueryDeveloperProfileUseCase queryDeveloperProfileUseCase;
    private final DeveloperProfileMapper developerProfileMapper;

    public DeveloperRepositoryController(
            QueryDeveloperProfileUseCase queryDeveloperProfileUseCase,
            DeveloperProfileMapper developerProfileMapper) {
        this.queryDeveloperProfileUseCase = queryDeveloperProfileUseCase;
        this.developerProfileMapper = developerProfileMapper;
    }

    @Operation(
        summary = "Get developer profile and repositories",
        description = "Retrieves a GitHub developer's profile information and list of public repositories"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved developer profile",
            content = @Content(schema = @Schema(implementation = DeveloperProfileResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid username format",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Developer not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many requests - rate limit exceeded"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "GitHub API unavailable",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping("/{username}")
    @RateLimit(
        capacity = "${rate-limit.developer.capacity}",
        refillTokens = "${rate-limit.developer.refillTokens}",
        refillPeriod = "${rate-limit.developer.refillPeriod}",
        refillUnit = "${rate-limit.developer.refillUnit}"
    )
    public ResponseEntity<DeveloperProfileResponse> getDeveloperProfile(
            @Parameter(description = "GitHub username (1-39 characters, alphanumeric with hyphens)", example = "octocat")
            @PathVariable @ValidGitHubUsername String username) {
        DeveloperProfile profile = queryDeveloperProfileUseCase.getDeveloperProfile(username);
        DeveloperProfileResponse response = developerProfileMapper.toDeveloperProfileResponse(profile);
        return ResponseEntity.ok(response);
    }
}