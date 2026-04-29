// service_spring/src/main/java/com/bayesian/controller/AssimilationController.java

package com.bayesian.controller;

import com.bayesian.dto.request.AssimilationRequest;
import com.bayesian.dto.response.AssimilationResponse;
import com.bayesian.service.AssimilationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/assimilation")
public class AssimilationController {
    
    @Autowired
    private AssimilationService assimilationService;
    
    @PostMapping("/execute")
    public CompletableFuture<AssimilationResponse> executeAssimilation(@RequestBody AssimilationRequest request) {
        log.info("接收同化请求，JobID: {}", request.getJobId());
        return assimilationService.executeAssimilation(request);
    }
    
    @PostMapping("/batch")
    public CompletableFuture<String> batchAssimilation(@RequestBody BatchRequest request) {
        log.info("接收批量同化请求，任务数: {}", request.getJobs().size());
        // 实现批量处理逻辑
        return CompletableFuture.completedFuture("Batch processing not implemented");
    }
    
    @PostMapping("/variance")
    public CompletableFuture<AssimilationResponse> getVariance(@RequestBody AssimilationRequest request) {
        log.info("接收方差场请求，JobID: {}", request.getJobId());
        // 实现方差场计算逻辑
        return assimilationService.executeAssimilation(request);
    }
    
    @GetMapping("/health")
    public CompletableFuture<Boolean> healthCheck() {
        log.info("健康检查");
        return assimilationService.checkPythonHealth();
    }
    
    // 批量请求类
    public static class BatchRequest {
        private java.util.List<AssimilationRequest> jobs;
        
        public java.util.List<AssimilationRequest> getJobs() {
            return jobs;
        }
        
        public void setJobs(java.util.List<AssimilationRequest> jobs) {
            this.jobs = jobs;
        }
    }
}