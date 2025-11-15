package com.branch.repository.api.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for GitHubCacheKeyGenerator.
 * Tests cache key generation for different method signatures and parameter combinations.
 */
class GitHubCacheKeyGeneratorTest {

    private GitHubCacheKeyGenerator keyGenerator;
    private Object target;

    @BeforeEach
    void setUp() {
        keyGenerator = new GitHubCacheKeyGenerator();
        target = new TestService();
    }

    @Test
    void generate_shouldIncludeMethodName() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("getUserProfile", String.class);
        Object[] params = {"octocat"};

        // When
        Object key = keyGenerator.generate(target, method, params);

        // Then
        assertEquals("getUserProfile::octocat", key);
    }

    @Test
    void generate_shouldDifferentiateMethodsWithSameParameter() throws NoSuchMethodException {
        // Given
        Method method1 = TestService.class.getMethod("getUserProfile", String.class);
        Method method2 = TestService.class.getMethod("getUserRepositories", String.class);
        Object[] params = {"octocat"};

        // When
        Object key1 = keyGenerator.generate(target, method1, params);
        Object key2 = keyGenerator.generate(target, method2, params);

        // Then
        assertNotEquals(key1, key2, "Different methods should generate different cache keys");
        assertEquals("getUserProfile::octocat", key1);
        assertEquals("getUserRepositories::octocat", key2);
    }

    @Test
    void generate_shouldHandleMultipleParameters() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("searchRepositories", String.class, String.class, Integer.class);
        Object[] params = {"octocat", "public", 10};

        // When
        Object key = keyGenerator.generate(target, method, params);

        // Then
        assertEquals("searchRepositories::octocat::public::10", key);
    }

    @Test
    void generate_shouldHandleNullParameter() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("getUserProfile", String.class);
        Object[] params = {null};

        // When
        Object key = keyGenerator.generate(target, method, params);

        // Then
        assertEquals("getUserProfile::null", key);
    }

    @Test
    void generate_shouldHandleNoParameters() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("getAllUsers");
        Object[] params = {};

        // When
        Object key = keyGenerator.generate(target, method, params);

        // Then
        assertEquals("getAllUsers", key);
    }

    @Test
    void generate_shouldHandleMixedNullAndNonNullParameters() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("searchRepositories", String.class, String.class, Integer.class);
        Object[] params = {"octocat", null, 10};

        // When
        Object key = keyGenerator.generate(target, method, params);

        // Then
        assertEquals("searchRepositories::octocat::null::10", key);
    }

    @Test
    void generate_shouldGenerateConsistentKeysForSameInputs() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("getUserProfile", String.class);
        Object[] params = {"octocat"};

        // When
        Object key1 = keyGenerator.generate(target, method, params);
        Object key2 = keyGenerator.generate(target, method, params);

        // Then
        assertEquals(key1, key2, "Same inputs should generate identical cache keys");
    }

    @Test
    void generate_shouldDifferentiateDifferentParameterValues() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("getUserProfile", String.class);
        Object[] params1 = {"octocat"};
        Object[] params2 = {"torvalds"};

        // When
        Object key1 = keyGenerator.generate(target, method, params1);
        Object key2 = keyGenerator.generate(target, method, params2);

        // Then
        assertNotEquals(key1, key2, "Different parameter values should generate different cache keys");
        assertEquals("getUserProfile::octocat", key1);
        assertEquals("getUserProfile::torvalds", key2);
    }

    @Test
    void generate_shouldHandleComplexObjectParameters() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("getCustomData", Object.class);
        Object customObject = new CustomObject("test", 123);
        Object[] params = {customObject};

        // When
        Object key = keyGenerator.generate(target, method, params);

        // Then
        assertTrue(key.toString().startsWith("getCustomData::"));
        assertTrue(key.toString().contains("CustomObject"));
    }

    @Test
    void generate_shouldUseDoubleColonDelimiter() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("searchRepositories", String.class, String.class, Integer.class);
        Object[] params = {"user", "type", 5};

        // When
        Object key = keyGenerator.generate(target, method, params);

        // Then
        String keyString = key.toString();
        long delimiterCount = keyString.chars().filter(ch -> ch == ':').count();
        assertEquals(6, delimiterCount, "Should have 3 double-colon delimiters (6 colons total)");
    }

    /**
     * Test service class with various method signatures for testing.
     */
    public static class TestService {
        public String getUserProfile(String username) {
            return username;
        }

        public String[] getUserRepositories(String username) {
            return new String[]{username};
        }

        public String searchRepositories(String username, String visibility, Integer limit) {
            return username + visibility + limit;
        }

        public String getAllUsers() {
            return "all";
        }

        public Object getCustomData(Object data) {
            return data;
        }
    }

    /**
     * Custom object for testing parameter serialization.
     */
    private static class CustomObject {
        private final String name;
        private final int value;

        CustomObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return "CustomObject{name='" + name + "', value=" + value + "}";
        }
    }
}