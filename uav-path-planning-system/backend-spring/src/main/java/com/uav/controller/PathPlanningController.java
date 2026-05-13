package com.uav.controller;

import com.uav.model.PathPlan;
import com.uav.utils.PythonAlgorithmUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/path-planning")
public class PathPlanningController {

    private final PythonAlgorithmUtil pythonAlgorithmUtil;

    public PathPlanningController(PythonAlgorithmUtil pythonAlgorithmUtil) {
        this.pythonAlgorithmUtil = pythonAlgorithmUtil;
    }
    
    @PostMapping("/plan")
    public Map<String, Object> planPath(@RequestBody Map<String, Object> request) {
        try {
            log.info("收到路径规划请求: {}", request);
            
            String tasks = request.getOrDefault("tasks", "").toString();
            String drones = request.getOrDefault("drones", "").toString();
            String weatherData = request.getOrDefault("weatherData", "").toString();
            
            String result = pythonAlgorithmUtil.planPath(tasks, drones, weatherData);
            
            log.info("路径规划完成，结果: {}", result);
            
            return Map.of(
                "code", 200,
                "data", result
            );
            
        } catch (Exception e) {
            log.error("路径规划失败", e);
            return Map.of(
                "code", 500,
                "message", "路径规划处理失败"
            );
        }
    }
    
    @GetMapping("/history")
    public Map<String, Object> getPlanningHistory() {
        return Map.of(
            "code", 200,
            "data", List.of()
        );
    }
    
    @PostMapping("/save")
    public Map<String, Object> savePathPlan(@RequestBody PathPlan plan) {
        return Map.of(
            "code", 200,
            "message", "规划方案保存成功"
        );
    }
    
    @GetMapping("/detail/{id}")
    public Map<String, Object> getPathPlanDetail(@PathVariable Long id) {
        return Map.of(
            "code", 200,
            "data", Map.of()
        );
    }
}
