package com.branch.repository.api.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Base test class providing utility methods for loading JSON resource files.
 * Extend this class to gain access to resource loading functionality.
 */
public abstract class BaseResourceTest {

    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUpObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Loads a JSON resource file and deserializes to a Java object.
     *
     * @param resourceName the name of the resource file (e.g., "user-response.json")
     * @param type the class type to deserialize to
     * @param <T> the type parameter
     * @return the deserialized object
     * @throws IOException if resource cannot be loaded or deserialized
     */
    protected <T> T loadResource(String resourceName, Class<T> type) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Resource not found: " + resourceName);
            return objectMapper.readValue(is, type);
        }
    }

    /**
     * Loads a JSON resource file and deserializes to a generic type using TypeReference.
     * Use this for collections and other generic types.
     *
     * @param resourceName the name of the resource file (e.g., "repos-response.json")
     * @param typeReference the TypeReference for the generic type
     * @param <T> the type parameter
     * @return the deserialized object
     * @throws IOException if resource cannot be loaded or deserialized
     */
    protected <T> T loadResource(String resourceName, TypeReference<T> typeReference) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Resource not found: " + resourceName);
            return objectMapper.readValue(is, typeReference);
        }
    }
}