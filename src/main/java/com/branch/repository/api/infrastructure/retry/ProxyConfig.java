package com.branch.repository.api.infrastructure.retry;

import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AOP proxy creation.
 * Sets up BeanNameAutoProxyCreator to automatically apply retry interceptor to specified beans.
 */
@Configuration
public class ProxyConfig {

    /**
     * Creates a BeanNameAutoProxyCreator that applies the retry interceptor to beans
     * with names matching the pattern "*GitHubService".
     *
     * @return configured BeanNameAutoProxyCreator
     */
    @Bean
    public BeanNameAutoProxyCreator beanNameAutoProxyCreator() {
        BeanNameAutoProxyCreator proxyCreator = new BeanNameAutoProxyCreator();
        proxyCreator.setBeanNames("*GitHubService");
        proxyCreator.setInterceptorNames("retryInterceptor");
        return proxyCreator;
    }
}
