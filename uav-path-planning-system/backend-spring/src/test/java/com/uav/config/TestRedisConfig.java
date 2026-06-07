package com.uav.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * 测试专用 Redis 配置 — 提供 mock RedisTemplate 代替真实 Redis 连接
 */
@TestConfiguration
@Profile("test")
public class TestRedisConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }
}
