package com.uav.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;

/**
 * 测试专用基础设施配置 — 提供 mock Bean 代替真实连接
 */
@TestConfiguration
@Profile("test")
public class TestRedisConfig {

    @SuppressWarnings("unchecked")
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean("meteorForecastRetry")
    @Primary
    public Retry meteorForecastRetry() {
        return mock(Retry.class);
    }

    @Bean("pathPlanningRetry")
    @Primary
    public Retry pathPlanningRetry() {
        return mock(Retry.class);
    }

    @Bean("dataAssimilationRetry")
    @Primary
    public Retry dataAssimilationRetry() {
        return mock(Retry.class);
    }

    @Bean("meteorForecastCircuitBreaker")
    @Primary
    public CircuitBreaker meteorForecastCircuitBreaker() {
        return mock(CircuitBreaker.class);
    }

    @Bean("pathPlanningCircuitBreaker")
    @Primary
    public CircuitBreaker pathPlanningCircuitBreaker() {
        return mock(CircuitBreaker.class);
    }

    @Bean("dataAssimilationCircuitBreaker")
    @Primary
    public CircuitBreaker dataAssimilationCircuitBreaker() {
        return mock(CircuitBreaker.class);
    }

    @Bean("resilientRestTemplate")
    @Primary
    public RestTemplate resilientRestTemplate() {
        return mock(RestTemplate.class);
    }
}
