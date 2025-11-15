package com.branch.repository.api.infrastructure.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * Custom cache key generator for GitHub API calls.
 * Generates composite cache keys that include method name and all parameters.
 * This ensures cache isolation between different methods and provides extensibility
 * for future query parameters (e.g., pagination, filters, include_private, etc.).
 *
 * <p>Key format: "methodName::param1::param2::..."
 *
 * <p>Examples:
 * - getUserProfile("octocat") → "getUserProfile::octocat"
 * - getUserRepositories("octocat") → "getUserRepositories::octocat"
 * - Future: getUserRepositories("octocat", "public") → "getUserRepositories::octocat::public"
 */
@Component("gitHubCacheKeyGenerator")
public class GitHubCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringJoiner keyJoiner = new StringJoiner("::");

        // Include method name to differentiate between different GitHub API calls
        keyJoiner.add(method.getName());

        // Include all parameters in the cache key
        for (Object param : params) {
            keyJoiner.add(param != null ? param.toString() : "null");
        }

        return keyJoiner.toString();
    }
}