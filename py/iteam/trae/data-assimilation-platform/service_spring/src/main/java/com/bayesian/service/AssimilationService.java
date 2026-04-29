// service_spring/src/main/java/com/bayesian/service/AssimilationService.java

package com.bayesian.service;

import com.bayesian.client.PythonServiceClient;
import com.bayesian.dto.request.AssimilationRequest;
import com.bayesian.dto.response.AssimilationResponse;
import com.bayesian.entity.AssimilationJob;
import com.bayesian.exception.AssimilationException;
import com.bayesian.exception.DegradedModeException;
import com.bayesian.mapper.JobMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class AssimilationService {
    
    @Autowired
    private PythonServiceClient pythonClient;
    
    @Autowired
    private JobMapper jobMapper;
    
    @Autowired
    private CacheService cacheService;
    
    /**
     * 主入口：执行同化计算（全防护）
     * 
     * 防护层级（从内到外）：
     * 1. TimeLimiter - 超时控制（25秒）
     * 2. RateLimiter - 限流（10次/秒）
     * 3. Bulkhead - 隔离舱（10并发）
     * 4. Retry - 重试（2次）
     * 5. CircuitBreaker - 熔断（最外层）
     */
    @CircuitBreaker(name = "pythonAssimilation", fallbackMethod = "assimilationFallback")
    @Retry(name = "assimilationRetry")
    @Bulkhead(name = "pythonServiceBulkhead", type = Bulkhead.Type.THREADPOOL)
    @RateLimiter(name = "assimilationLimiter")
    @TimeLimiter(name = "assimilationTimeLimiter")  // 需在配置中添加
    public CompletableFuture<AssimilationResponse> executeAssimilation(AssimilationRequest request) {
        log.info("[Assimilation] 开始执行，JobID: {}", request.getJobId());
        
        // 记录作业状态
        saveJobStatus(request.getJobId(), "PROCESSING");
        
        return pythonClient.computeAssimilation(request)
            .doOnSuccess(result -> {
                log.info("[Assimilation] 成功完成，JobID: {}", request.getJobId());
                saveJobStatus(request.getJobId(), "COMPLETED", result);
                
                // 缓存结果（用于降级时返回）
                cacheService.cacheResult(request.getJobId(), result);
            })
            .doOnError(error -> {
                log.error("[Assimilation] 执行失败，JobID: {}, 错误: {}", 
                    request.getJobId(), error.getMessage());
                saveJobStatus(request.getJobId(), "FAILED", null, error.getMessage());
            })
            .toFuture();
    }
    
    /**
     * 熔断降级方法
     * 
     * 策略优先级：
     * 1. 返回缓存的同化结果（如果存在且未过期）
     * 2. 使用背景场作为分析场（直通模式）
     * 3. 返回错误，建议客户端稍后重试
     */
    private CompletableFuture<AssimilationResponse> assimilationFallback(
            AssimilationRequest request, 
            Exception exception) {
        
        log.warn("[Assimilation] 触发熔断降级，JobID: {}, 原因: {}", 
            request.getJobId(), exception.getClass().getSimpleName());
        
        // 策略1：尝试获取缓存
        AssimilationResponse cached = cacheService.getCachedResult(request.getJobId());
        if (cached != null && !isCacheExpired(cached)) {
            log.info("[Assimilation] 返回缓存结果，JobID: {}", request.getJobId());
            cached.setStatus("DEGRADED_CACHED");
            cached.setMessage("服务熔断中，返回最近一次成功的同化结果");
            return CompletableFuture.completedFuture(cached);
        }
        
        // 策略2：背景场直通（仅当请求允许降级时）
        if (request.isAllowDegraded()) {
            log.info("[Assimilation] 使用背景场直通，JobID: {}", request.getJobId());
            
            AssimilationResponse degradedResponse = AssimilationResponse.builder()
                .jobId(request.getJobId())
                .status("DEGRADED_BACKGROUND")
                .message("Python计算服务熔断，使用背景场作为分析场（无同化）")
                .analysisField(request.getBackgroundField())
                .varianceField(request.getBackgroundError())
                .timestamp(LocalDateTime.now())
                .computationTimeMs(0)
                .build();
            
            return CompletableFuture.completedFuture(degradedResponse);
        }
        
        // 策略3：拒绝服务
        log.error("[Assimilation] 无法降级，拒绝请求，JobID: {}", request.getJobId());
        return CompletableFuture.failedFuture(
            new DegradedModeException("同化服务暂时不可用，请稍后重试", exception)
        );
    }
    
    /**
     * 专用重试降级（重试耗尽后）
     */
    private CompletableFuture<AssimilationResponse> assimilationFallback(
            AssimilationRequest request, 
            Retry.RetryException retryException) {
        
        log.error("[Assimilation] 重试耗尽，JobID: {}", request.getJobId());
        return assimilationFallback(request, (Exception) retryException);
    }
    
    /**
     * 专用超时降级
     */
    private CompletableFuture<AssimilationResponse> assimilationFallback(
            AssimilationRequest request, 
            TimeoutException timeoutException) {
        
        log.error("[Assimilation] 计算超时，JobID: {}", request.getJobId());
        // 超时后尝试返回部分结果或缓存
        return assimilationFallback(request, (Exception) timeoutException);
    }
    
    /**
     * 批量处理（不同熔断配置）
     */
    @CircuitBreaker(name = "pythonBatch", fallbackMethod = "batchFallback")
    @Retry(name = "batchRetry")
    public CompletableFuture<BatchResponse> executeBatch(BatchRequest request) {
        // 批量处理逻辑...
        return null;
    }
    
    /**
     * 健康检查（轻量级，用于恢复探测）
     */
    @CircuitBreaker(name = "pythonHealthCheck")
    public CompletableFuture<Boolean> checkPythonHealth() {
        return pythonClient.healthCheck().toFuture();
    }
    
    // 辅助方法
    private void saveJobStatus(String jobId, String status) {
        // 持久化状态...
    }
    
    private void saveJobStatus(String jobId, String status, AssimilationResponse result) {
        // 持久化状态和结果...
    }
    
    private void saveJobStatus(String jobId, String status, AssimilationResponse result, String error) {
        // 持久化错误...
    }
    
    private boolean isCacheExpired(AssimilationResponse cached) {
        // 缓存有效期5分钟
        return cached.getTimestamp().plusMinutes(5).isBefore(LocalDateTime.now());
    }
}
