package com.uav.platform.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/platform")
public class PlatformController {
    
    @Resource
    private RestTemplate restTemplate;
    
    @Value("${services.wrf-processor.url}")
    private String wrfProcessorUrl;
    
    @Value("${services.data-assimilation.url}")
    private String dataAssimilationUrl;
    
    @Value("${services.meteor-forecast.url}")
    private String meteorForecastUrl;
    
    @Value("${services.path-planning.url}")
    private String pathPlanningUrl;
    
    /**
     * 完整路径规划流程
     * @param request 规划请求
     * @return 规划结果
     */
    @PostMapping("/plan")
    public Map<String, Object> plan(@RequestBody Map<String, Object> request) {
        try {
            // 1. 获取气象数据
            Map<String, Object> weatherResponse = restTemplate.postForObject(
                wrfProcessorUrl + "/parse",
                request.get("weatherData"),
                Map.class
            );
            
            if (weatherResponse == null || !Boolean.TRUE.equals(weatherResponse.get("success"))) {
                return Map.of(
                    "success", false,
                    "error", "获取气象数据失败"
                );
            }
            
            // 2. 执行贝叶斯同化
            Map<String, Object> assimilationResponse = restTemplate.postForObject(
                dataAssimilationUrl + "/execute",
                weatherResponse.get("data"),
                Map.class
            );
            
            if (assimilationResponse == null || !Boolean.TRUE.equals(assimilationResponse.get("success"))) {
                return Map.of(
                    "success", false,
                    "error", "执行贝叶斯同化失败"
                );
            }
            
            // 3. 执行气象预测与订正
            Map<String, Object> forecastResponse = restTemplate.postForObject(
                meteorForecastUrl + "/predict",
                assimilationResponse.get("data"),
                Map.class
            );
            
            if (forecastResponse == null || !Boolean.TRUE.equals(forecastResponse.get("success"))) {
                return Map.of(
                    "success", false,
                    "error", "执行气象预测失败"
                );
            }
            
            // 4. 执行路径规划
            Map<String, Object> planningRequest = Map.of(
                "drones", request.get("drones"),
                "tasks", request.get("tasks"),
                "weather_data", forecastResponse.get("data"),
                "obstacles", request.get("obstacles"),
                "no_fly_zones", request.get("noFlyZones")
            );
            
            Map<String, Object> planningResponse = restTemplate.postForObject(
                pathPlanningUrl + "/full",
                planningRequest,
                Map.class
            );
            
            if (planningResponse == null || !Boolean.TRUE.equals(planningResponse.get("success"))) {
                return Map.of(
                    "success", false,
                    "error", "执行路径规划失败"
                );
            }
            
            return Map.of(
                "success", true,
                "data", planningResponse.get("data")
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 获取综合气象数据
     * @param request 请求参数
     * @return 气象数据
     */
    @GetMapping("/weather")
    public Map<String, Object> getWeather(@RequestParam Map<String, String> request) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                wrfProcessorUrl + "/data?fileId={fileId}",
                Map.class,
                request.get("fileId")
            );
            
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return Map.of(
                    "success", false,
                    "error", "获取气象数据失败"
                );
            }
            
            return response;
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 任务管理
     * @param request 任务请求
     * @return 任务结果
     */
    @PostMapping("/task")
    public Map<String, Object> manageTask(@RequestBody Map<String, Object> request) {
        try {
            // 这里可以实现任务的创建、更新、删除等操作
            return Map.of(
                "success", true,
                "message", "任务管理成功"
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 无人机管理
     * @param request 无人机请求
     * @return 无人机结果
     */
    @GetMapping("/drones")
    public Map<String, Object> getDrones(@RequestParam Map<String, String> request) {
        try {
            // 这里可以实现无人机的查询、状态更新等操作
            return Map.of(
                "success", true,
                "data", Map.of(
                    "drones", Map.of()
                )
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}