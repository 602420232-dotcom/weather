package com.uav.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

/**
 * 气象预测服务Feign Client
 * 用于声明式调用meteor-forecast-service服务
 */
@FeignClient(name = "meteor-forecast-service", url = "${services.meteor-forecast.url:http://meteor-forecast:8082}",
        fallback = MeteorForecastClientFallback.class)
public interface MeteorForecastClient {

    /**
     * 获取气象预报
     */
    @GetMapping("/api/forecast")
    Map<String, Object> getForecast(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("hours") Integer hours);

    /**
     * 获取气象预报（详细参数）
     */
    @PostMapping("/api/forecast/detailed")
    Map<String, Object> getDetailedForecast(@RequestBody Map<String, Object> request);

    /**
     * 获取实时气象数据
     */
    @GetMapping("/api/forecast/realtime")
    Map<String, Object> getRealtimeWeather(@RequestParam("lat") Double lat, @RequestParam("lng") Double lng);

    /**
     * 健康检查
     */
    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
