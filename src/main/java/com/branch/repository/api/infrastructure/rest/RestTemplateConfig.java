package com.branch.repository.api.infrastructure.rest;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for RestTemplate with custom error handling and connection pooling.
 * Configures RestTemplate to use RetryableResponseErrorHandler which
 * automatically converts retryable HTTP errors into RetryableHttpException.
 * Also configures connection pooling and timeouts for improved performance and reliability.
 */
@Configuration
public class RestTemplateConfig {

    @Bean(name = "githubRestTemplate")
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Configure connection pool
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);  // Max total connections
        connectionManager.setDefaultMaxPerRoute(20);  // Max connections per route

        // Configure connection settings
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setSocketTimeout(Timeout.ofSeconds(10))
            .setTimeToLive(30, TimeUnit.SECONDS)
            .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        // Configure request settings
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(5))
            .build();

        // Build HTTP client
        HttpClient httpClient = HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(Timeout.ofSeconds(30))
            .build();

        // Create request factory with the configured HTTP client
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
            .requestFactory(() -> factory)
            .errorHandler(new RetryableResponseErrorHandler())
            .build();
    }
}