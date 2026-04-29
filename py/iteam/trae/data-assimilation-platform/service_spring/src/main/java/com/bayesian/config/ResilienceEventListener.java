// service_spring/src/main/java/com/bayesian/config/ResilienceEventListener.java

package com.bayesian.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class ResilienceEventListener {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private RetryRegistry retryRegistry;
    
    @Autowired
    private AlertService alertService;
    
    @PostConstruct
    public void registerEventListeners() {
        // 监听熔断器状态转换
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            cb.getEventPublisher()
                .onStateTransition(this::handleStateTransition)
                .onError(this::handleCircuitBreakerError)
                .onSlowCall(this::handleSlowCall);
        });
        
        // 监听重试事件
        retryRegistry.getAllRetries().forEach(retry -> {
            retry.getEventPublisher()
                .onRetry(this::handleRetryEvent);
        });
    }
    
    private void handleStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        String cbName = event.getCircuitBreakerName();
        String fromState = event.getStateTransition().getFromState().name();
        String toState = event.getStateTransition().getToState().name();
        
        log.warn("[CircuitBreaker] {} 状态转换: {} -> {}", cbName, fromState, toState);
        
        // 关键状态转换发送告警
        if ("CLOSED".equals(fromState) && "OPEN".equals(toState)) {
            alertService.sendAlert("CRITICAL", 
                String.format("熔断器 %s 已开启，Python服务可能故障", cbName));
        }
        
        if ("OPEN".equals(fromState) && "HALF_OPEN".equals(toState)) {
            alertService.sendAlert("INFO", 
                String.format("熔断器 %s 进入半开状态，正在探测恢复", cbName));
        }
        
        if ("HALF_OPEN".equals(fromState) && "CLOSED".equals(toState)) {
            alertService.sendAlert("RECOVERED", 
                String.format("熔断器 %s 已关闭，服务恢复正常", cbName));
        }
    }
    
    private void handleCircuitBreakerError(io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent event) {
        log.error("[CircuitBreaker] {} 调用失败: {}", 
            event.getCircuitBreakerName(), 
            event.getThrowable().getMessage());
    }
    
    private void handleSlowCall(io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSlowCallEvent event) {
        log.warn("[CircuitBreaker] {} 检测到慢调用", event.getCircuitBreakerName());
    }
    
    private void handleRetryEvent(RetryOnRetryEvent event) {
        log.info("[Retry] {} 第 {}/{} 次重试，异常: {}", 
            event.getName(),
            event.getNumberOfRetryAttempts(),
            event.getMaxAttempts(),
            event.getLastThrowable().getClass().getSimpleName());
    }
}
