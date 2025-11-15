package com.branch.repository.api.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for GitHub API responses using Caffeine.
 * Caffeine provides automatic expiration, size-based eviction, and better performance than ConcurrentHashMap.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String GITHUB_USER_PROFILE_CACHE = "githubUserProfile";
    public static final String GITHUB_USER_REPOSITORIES_CACHE = "githubUserRepositories";

    @Value("${cache.github.ttl-minutes:60}")
    private long ttlMinutes;

    @Value("${cache.github.max-size:1000}")
    private long maxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                GITHUB_USER_PROFILE_CACHE,
                GITHUB_USER_REPOSITORIES_CACHE
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Cache entry removed - key: {}, cause: {}", key, cause);
                    }
                }));

        log.info("Caffeine cache manager initialized with TTL={}min, maxSize={}, caches: {}",
                ttlMinutes, maxSize, cacheManager.getCacheNames());

        return cacheManager;
    }

}