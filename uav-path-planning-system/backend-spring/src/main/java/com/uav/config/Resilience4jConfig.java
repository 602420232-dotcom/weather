package com.uav.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import java.time.Duration;

/**
 * Resilience4j熔断降级配置
 * 使用Resilience4j 2.x API
 */
@Configuration
public class Resilience4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfig.class);

    /**
     * 通用熔断配置
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig cbConfig = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .minimumNumberOfCalls(10)
            .slidingWindowSize(100)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .slowCallRateThreshold(50)
            .recordExceptions(Exception.class)
            .build();
        
        return CircuitBreakerRegistry.of(cbConfig);
    }

    /**
     * 通用重试配置
     */
    @Bean
    public RetryRegistry retryRegistry() {
        io.github.resilience4j.retry.RetryConfig config = 
            io.github.resilience4j.retry.RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryOnException(e -> true)
            .build();
        
        return RetryRegistry.of(config);
    }

    /**
     * 通用超时配置
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))
            .cancelRunningFuture(true)
            .build();
        
        return TimeLimiterRegistry.of(config);
    }

    /**
     * 路径规划服务熔断
     */
    @Bean
    public CircuitBreaker pathPlanningCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("pathPlanning");
    }

    /**
     * 气象预测服务熔断
     */
    @Bean
    public CircuitBreaker meteorForecastCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("meteorForecast");
    }

    /**
     * 数据同化服务熔断
     */
    @Bean
    public CircuitBreaker dataAssimilationCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("dataAssimilation");
    }

    /**
     * 熔断状态监控
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                log.info("CircuitBreaker added: {}", entryAddedEvent.getAddedEntry().getName());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemovedEvent) {
                log.info("CircuitBreaker removed: {}", entryRemovedEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("CircuitBreaker replaced: {}", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
}