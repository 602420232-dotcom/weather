package com.bayesian.config;

import com.bayesian.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("同化平台配置测试")
class DataAssimilationConfigTests {

    @Test
    @DisplayName("WebClientConfig创建")
    void testWebClientConfig() {
        WebClientConfig config = new WebClientConfig();
        assertNotNull(config.webClient());
    }

    @Test
    @DisplayName("ResilienceEventPublisher创建")
    void testResilienceEventPublisher() {
        ResilienceEventListener listener = new ResilienceEventListener();
        assertDoesNotThrow(listener::resilienceEventPublisher);
    }

    @Test
    @DisplayName("ResilienceConfig创建")
    void testResilienceConfig() {
        ResilienceConfig config = new ResilienceConfig();
        assertDoesNotThrow(() -> config.circuitBreakerRegistry());
    }

    @Test
    @DisplayName("ProtocolConfig创建")
    void testProtocolConfig() {
        ProtocolConfig config = new ProtocolConfig();
        assertDoesNotThrow(() -> config.protocolBufferDecoder());
    }

    @Test
    @DisplayName("CacheService缓存设置")
    void testCacheServiceSet() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);

        CacheService cacheService = new CacheService(template);
        assertDoesNotThrow(() -> cacheService.cache("test-key", "test-value", 300));
    }

    @Test
    @DisplayName("CacheService缓存读取失败返回null")
    void testCacheServiceGetWithError() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        when(template.opsForValue()).thenThrow(new RuntimeException("Redis连接失败"));

        CacheService cacheService = new CacheService(template);
        assertNull(cacheService.get("test-key"));
    }

    @Test
    @DisplayName("CacheService缓存删除")
    void testCacheServiceEvict() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);

        CacheService cacheService = new CacheService(template);
        assertDoesNotThrow(() -> cacheService.evict("test-key"));
    }
}