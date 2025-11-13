package com.branch.repository.api.infrastructure.retry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuration for retry interceptors.
 * Provides RetryOperationsInterceptor that can be applied to beans via BeanNameAutoProxyCreator.
 */
@Configuration
public class RetryInterceptorConfig {

    /**
     * Creates a RetryOperationsInterceptor using the configured RetryTemplate.
     * This interceptor can be applied to beans via BeanNameAutoProxyCreator.
     *
     * @param retryTemplate the retry template to use
     * @return configured RetryOperationsInterceptor
     */
    @Bean
    public RetryOperationsInterceptor retryInterceptor(RetryTemplate retryTemplate) {
        RetryOperationsInterceptor interceptor = new RetryOperationsInterceptor();
        interceptor.setRetryOperations(retryTemplate);
        return interceptor;
    }
}