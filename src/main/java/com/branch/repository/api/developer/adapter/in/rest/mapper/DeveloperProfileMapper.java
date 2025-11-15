package com.branch.repository.api.developer.adapter.in.rest.mapper;

import com.branch.repository.api.developer.adapter.in.rest.model.DeveloperProfileResponse;
import com.branch.repository.api.developer.adapter.in.rest.model.DeveloperRepositoryResponse;
import com.branch.repository.api.developer.domain.model.DeveloperProfile;
import com.branch.repository.api.developer.domain.model.DeveloperRepository;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting domain models to REST response DTOs.
 * Handles the transformation from hexagonal architecture domain layer to REST adapter layer.
 */
@Mapper(componentModel = "spring")
public interface DeveloperProfileMapper {

    /**
     * Maps DeveloperProfile domain model to DeveloperProfileResponse DTO.
     * Date formatting is handled by Jackson serialization (see JacksonConfig).
     *
     * @param profile the domain model
     * @return the REST response DTO
     */
    DeveloperProfileResponse toDeveloperProfileResponse(DeveloperProfile profile);

    /**
     * Maps Repository domain model to DeveloperRepositoryResponse DTO.
     *
     * @param developerRepository the domain model
     * @return the REST response DTO
     */
    DeveloperRepositoryResponse toDeveloperRepositoryResponse(DeveloperRepository developerRepository);
}