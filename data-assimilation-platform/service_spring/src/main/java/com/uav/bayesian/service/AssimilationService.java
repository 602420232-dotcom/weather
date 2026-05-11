package com.uav.bayesian.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AssimilationService {

    private static final Logger log = LoggerFactory.getLogger(AssimilationService.class);
    private final Map<String, Object> jobStore = new ConcurrentHashMap<>();
    private final PythonService pythonService;

    public AssimilationService(PythonService pythonService) {
        this.pythonService = pythonService;
    }

    public Map<String, Object> executeAssimilation(Map<String, Object> request) {
        String jobId = UUID.randomUUID().toString();
        log.info("开始同化任务 {}", jobId);
        
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("status", "running");
        Object algorithm = request != null ? request.get("algorithm") : null;
        if (algorithm != null) {
            jobStatus.put("algorithm", algorithm);
        }
        jobStore.put(jobId, jobStatus);
        
        try {
            String result = pythonService.executeAssimilation(request);
            
            Map<String, Object> completedStatus = new HashMap<>();
            completedStatus.put("status", "completed");
            completedStatus.put("result", result);
            jobStore.put(jobId, completedStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", "completed");
            response.put("result", result);
            return response;
            
        } catch (Exception e) {
            log.error("同化任务失败: jobId={}", jobId, e);
            
            Map<String, Object> failedStatus = new HashMap<>();
            failedStatus.put("status", "failed");
            failedStatus.put("error", "同化处理失败");
            jobStore.put(jobId, failedStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", "failed");
            response.put("error", "同化处理失败");
            return response;
        }
    }

    public Object getJobStatus(String jobId) {
        Object status = jobStore.get(jobId);
        if (status != null) {
            return status;
        }
        Map<String, Object> notFound = new HashMap<>();
        notFound.put("status", "not_found");
        return notFound;
    }
}
