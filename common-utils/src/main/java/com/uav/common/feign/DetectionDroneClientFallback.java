package com.uav.common.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 探测无人机服务降级实现
 */
@Slf4j
@Component
public class DetectionDroneClientFallback implements DetectionDroneClient {

    private Map<String, Object> degraded(String service) {
        log.warn("DetectionDroneClientFallback: {}降级执行", service);
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "degraded");
        fallback.put("message", "探测无人机服务暂不可用，请稍后重试");
        fallback.put("fallback", true);
        return fallback;
    }

    @Override public Map<String, Object> createMission(Map<String, Object> request) { return degraded("createMission"); }
    @Override public Map<String, Object> listMissions(Integer page, Integer size, String status) { return degraded("listMissions"); }
    @Override public Map<String, Object> getMissionStatus(Long id) { return degraded("getMissionStatus"); }
    @Override public Map<String, Object> updateMissionStatus(Long id, Map<String, Object> request) { return degraded("updateMissionStatus"); }
    @Override public Map<String, Object> getMissionData(Long id, Integer page, Integer size) { return degraded("getMissionData"); }
    @Override public Map<String, Object> uploadSample(Map<String, Object> request) { return degraded("uploadSample"); }
    @Override public Map<String, Object> getSampleHistory(String droneId, Integer hours) { return degraded("getSampleHistory"); }
    @Override public Map<String, Object> getVerticalProfile(Long id, Double layerSize) { return degraded("getVerticalProfile"); }
    @Override public Map<String, Object> health() { Map<String, Object> m = new HashMap<>(); m.put("status", "DOWN"); m.put("fallback", true); return m; }
}
