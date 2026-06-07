package com.uav.config;

import io.github.resilience4j.retry.Retry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

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

    /**
     * CircuitBreakerService 需要命名 Retry 实例，
     * Resilience4jConfig 只提供了 RetryRegistry 未提供命名 Retry Bean
     */
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
}
