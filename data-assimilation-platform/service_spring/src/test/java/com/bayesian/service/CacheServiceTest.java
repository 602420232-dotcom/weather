package com.bayesian.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService 单元测试")
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    @Nested
    @DisplayName("cache")
    class CacheTests {
        @Test
        @DisplayName("正常缓存写入")
        void shouldCacheSuccessfully() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            cacheService.cache("testKey", "testValue", 60);
            verify(valueOperations).set("testKey", "testValue", 60, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("Redis异常时不应抛出")
        void shouldNotThrowOnRedisError() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));
            assertDoesNotThrow(() -> cacheService.cache("key", "value", 60));
        }
    }

    @Nested
    @DisplayName("get")
    class GetTests {
        @Test
        @DisplayName("命中缓存返回数据")
        void shouldReturnCachedValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("key")).thenReturn("cachedValue");
            assertEquals("cachedValue", cacheService.get("key"));
        }

        @Test
        @DisplayName("未命中缓存返回null")
        void shouldReturnNullOnMiss() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("key")).thenReturn(null);
            assertNull(cacheService.get("key"));
        }

        @Test
        @DisplayName("Redis异常时返回null")
        void shouldReturnNullOnRedisError() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));
            assertNull(cacheService.get("key"));
        }
    }

    @Nested
    @DisplayName("evict")
    class EvictTests {
        @Test
        @DisplayName("正常删除缓存")
        void shouldEvictSuccessfully() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            assertDoesNotThrow(() -> cacheService.evict("key"));
            verify(redisTemplate).delete("key");
        }

        @Test
        @DisplayName("Redis异常时不应抛出")
        void shouldNotThrowOnRedisError() {
            doThrow(new RuntimeException("Redis down")).when(redisTemplate).delete(anyString());
            assertDoesNotThrow(() -> cacheService.evict("key"));
        }
    }
}
