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
 * CircuitBreakerService (common-utils) 需要的命名 Bean 全部由这里提供
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
    public Retry meteorForecastRetry() {
        return mock(Retry.class);
    }

    @Bean("pathPlanningRetry")
    public Retry pathPlanningRetry() { return mock(Retry.class); }

    @Bean("dataAssimilationRetry")
    public Retry dataAssimilationRetry() { return mock(Retry.class); }

    @Bean("meteorForecastCircuitBreaker")
    public CircuitBreaker meteorForecastCircuitBreaker() { return mock(CircuitBreaker.class); }

    @Bean("pathPlanningCircuitBreaker")
    public CircuitBreaker pathPlanningCircuitBreaker() { return mock(CircuitBreaker.class); }

    @Bean("dataAssimilationCircuitBreaker")
    public CircuitBreaker dataAssimilationCircuitBreaker() { return mock(CircuitBreaker.class); }

    @Bean("resilientRestTemplate")
    public RestTemplate resilientRestTemplate() { return mock(RestTemplate.class); }
}
