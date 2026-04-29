// service_spring/src/main/java/com/bayesian/controller/ResilienceController.java

package com.bayesian.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resilience")
@RequiredArgsConstructor
public class ResilienceController {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final ThreadPoolBulkheadRegistry bulkheadRegistry;
    
    /**
     * 获取所有熔断器状态
     */
    @GetMapping("/circuit-breakers")
    public Map<String, Object> getCircuitBreakers() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> {
                    CircuitBreaker.Metrics metrics = cb.getMetrics();
                    Map<String, Object> info = new HashMap<>();
                    info.put("state", cb.getState().name());
                    info.put("failureRate", metrics.getFailureRate());
                    info.put("slowCallRate", metrics.getSlowCallRate());
                    info.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
                    info.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
                    info.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
                    return info;
                }
            ));
    }
    
    /**
     * 手动切换熔断器状态（运维应急）
     */
    @PostMapping("/circuit-breakers/{name}/state")
    public String changeState(
            @PathVariable String name,
            @RequestParam String state) {
        
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        
        switch (state.toUpperCase()) {
            case "OPEN":
                cb.transitionToOpenState();
                return "熔断器 " + name + " 已强制开启";
            case "CLOSED":
                cb.transitionToClosedState();
                return "熔断器 " + name + " 已强制关闭";
            case "HALF_OPEN":
                cb.transitionToHalfOpenState();
                return "熔断器 " + name + " 已进入半开状态";
            default:
                return "未知状态: " + state;
        }
    }
    
    /**
     * 获取重试统计
     */
    @GetMapping("/retries")
    public Map<String, Object> getRetries() {
        return retryRegistry.getAllRetries().stream()
            .collect(Collectors.toMap(
                Retry::getName,
                retry -> {
                    Retry.Metrics metrics = retry.getMetrics();
                    Map<String, Object> info = new HashMap<>();
                    info.put("numberOfSuccessfulCallsWithoutRetry", 
                        metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt());
                    info.put("numberOfFailedCallsWithoutRetry", 
                        metrics.getNumberOfFailedCallsWithoutRetryAttempt());
                    info.put("numberOfSuccessfulCallsWithRetry", 
                        metrics.getNumberOfSuccessfulCallsWithRetryAttempt());
                    info.put("numberOfFailedCallsWithRetry", 
                        metrics.getNumberOfFailedCallsWithRetryAttempt());
                    return info;
                }
            ));
    }
    
    /**
     * 获取隔离舱状态
     */
    @GetMapping("/bulkheads")
    public Map<String, Object> getBulkheads() {
        return bulkheadRegistry.getAllBulkheads().stream()
            .collect(Collectors.toMap(
                ThreadPoolBulkhead::getName,
                bh -> {
                    ThreadPoolBulkhead.Metrics metrics = bh.getMetrics();
                    Map<String, Object> info = new HashMap<>();
                    info.put("maxAllowedConcurrentCalls", metrics.getMaxAllowedConcurrentCalls());
                    info.put("coreThreadPoolSize", metrics.getCoreThreadPoolSize());
                    info.put("threadPoolSize", metrics.getThreadPoolSize());
                    info.put("queueCapacity", metrics.getQueueCapacity());
                    info.put("queueDepth", metrics.getQueueDepth());
                    info.put("remainingQueueCapacity", metrics.getRemainingQueueCapacity());
                    return info;
                }
            ));
    }
}
