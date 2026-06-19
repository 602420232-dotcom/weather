package com.uav.common.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 探空气球服务降级实现
 * 当radiosonde-weather-service不可用时执行降级逻辑
 */
@Slf4j
@Component
public class RadiosondeWeatherClientFallback implements RadiosondeWeatherClient {

    @Override
    public Map<String, Object> getSoundingData(String stationId, Integer level) {
        log.warn("RadiosondeWeatherClientFallback: getSoundingData降级执行, stationId={}, level={}", stationId, level);
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "degraded");
        fallbackResponse.put("message", "探空气球数据服务暂不可用，请稍后重试");
        fallbackResponse.put("fallback", true);
        return fallbackResponse;
    }

    @Override
    public Map<String, Object> listStations(Integer page, Integer size) {
        log.warn("RadiosondeWeatherClientFallback: listStations降级执行, page={}, size={}", page, size);
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "degraded");
        fallbackResponse.put("message", "探空站列表服务暂不可用，请稍后重试");
        fallbackResponse.put("fallback", true);
        return fallbackResponse;
    }

    @Override
    public Map<String, Object> getSoundingDetail(Long id) {
        log.warn("RadiosondeWeatherClientFallback: getSoundingDetail降级执行, id={}", id);
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "degraded");
        fallbackResponse.put("message", "探空详情服务暂不可用，请稍后重试");
        fallbackResponse.put("fallback", true);
        return fallbackResponse;
    }

    @Override
    public Map<String, Object> getSkewTData(Long id) {
        log.warn("RadiosondeWeatherClientFallback: getSkewTData降级执行, id={}", id);
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "degraded");
        fallbackResponse.put("message", "斜温图数据服务暂不可用，请稍后重试");
        fallbackResponse.put("fallback", true);
        return fallbackResponse;
    }

    @Override
    public Map<String, Object> uploadSoundingData(Map<String, Object> request) {
        log.warn("RadiosondeWeatherClientFallback: uploadSoundingData降级执行, request={}", request);
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "degraded");
        fallbackResponse.put("message", "探空数据上传服务暂不可用，请稍后重试");
        fallbackResponse.put("fallback", true);
        return fallbackResponse;
    }

    @Override
    public Map<String, Object> health() {
        log.warn("RadiosondeWeatherClientFallback: health降级执行");
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "DOWN");
        fallbackResponse.put("fallback", true);
        return fallbackResponse;
    }
}
