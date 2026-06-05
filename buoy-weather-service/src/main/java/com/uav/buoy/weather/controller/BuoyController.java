package com.uav.buoy.weather.controller;

import com.uav.buoy.weather.model.BuoyData;
import com.uav.buoy.weather.repository.BuoyDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 浮标气象数据 REST Controller
 * 实现 BuoyWeatherClient 对应的 6 个端点
 */
@RestController
@RequestMapping("/api/buoy")
@RequiredArgsConstructor
@Slf4j
public class BuoyController {

    private final BuoyDataRepository repository;

    /**
     * GET /api/buoy/data?buoyId={buoyId}
     * 获取指定浮标的最新数据
     */
    @GetMapping("/data")
    public Map<String, Object> getBuoyData(
            @RequestParam(value = "buoyId", required = false) String buoyId) {
        if (buoyId != null && !buoyId.isBlank()) {
            Optional<BuoyData> data = repository.findLatestByBuoyId(buoyId);
            if (data.isPresent()) {
                return toMap(data.get(), "success");
            }
            return Map.of("status", "not_found",
                    "message", "未找到浮标 " + buoyId + " 的数据");
        }
        // 未指定 buoyId 时返回最新的一条数据
        Page<BuoyData> latest = repository.findAll(PageRequest.of(0, 1));
        if (latest.hasContent()) {
            return toMap(latest.getContent().get(0), "success");
        }
        return Map.of("status", "empty", "message", "暂无浮标数据");
    }

    /**
     * GET /api/buoy/list?page={page}&size={size}
     * 分页获取浮标数据列表
     */
    @GetMapping("/list")
    public Map<String, Object> listBuoys(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Page<BuoyData> dataPage = repository.findAll(
                PageRequest.of(page - 1, size));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("content", dataPage.getContent().stream()
                .map(this::toBriefMap).toList());
        result.put("totalElements", dataPage.getTotalElements());
        result.put("totalPages", dataPage.getTotalPages());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * GET /api/buoy/{id}
     * 获取指定 ID 的浮标数据详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getBuoyDetail(@PathVariable("id") Long id) {
        if (id == null) {
            return Map.of("status", "error", "message", "ID 不能为空");
        }
        return repository.findById(id)
                .map(data -> toMap(data, "success"))
                .orElse(Map.of("status", "not_found",
                        "message", "未找到 ID=" + id + " 的浮标数据"));
    }

    /**
     * GET /api/buoy/realtime?buoyId={buoyId}
     * 获取浮标实时数据（最近一条记录）
     */
    @GetMapping("/realtime")
    public Map<String, Object> getRealtimeData(
            @RequestParam(value = "buoyId") String buoyId) {
        Optional<BuoyData> data = repository.findLatestByBuoyId(buoyId);
        return data.map(d -> {
            Map<String, Object> result = toMap(d, "success");
            result.put("realtime", true);
            return result;
        }).orElse(Map.of("status", "not_found",
                "message", "未找到浮标 " + buoyId + " 的实时数据"));
    }

    /**
     * POST /api/buoy/upload
     * 上传浮标观测数据
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadBuoyData(
            @RequestBody Map<String, Object> request) {
        try {
            BuoyData data = BuoyData.builder()
                    .buoyId(String.valueOf(request.get("buoyId")))
                    .buoyName((String) request.getOrDefault("buoyName", null))
                    .longitude(toDouble(request.get("longitude")))
                    .latitude(toDouble(request.get("latitude")))
                    .windSpeed(toDouble(request.get("windSpeed")))
                    .windDirection(toDouble(request.get("windDirection")))
                    .temperature(toDouble(request.get("temperature")))
                    .pressure(toDouble(request.get("pressure")))
                    .humidity(toDouble(request.get("humidity")))
                    .waveHeight(toDouble(request.get("waveHeight")))
                    .waterTemperature(toDouble(request.get("waterTemperature")))
                    .collectTime(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            if (data == null) {
                return Map.of("status", "error", "message", "浮标数据不能为空");
            }
            BuoyData saved = repository.save(data);
            log.info("浮标数据上传成功: buoyId={}, id={}",
                    saved.getBuoyId(), saved.getId());
            Map<String, Object> result = toMap(saved, "success");
            result.put("message", "浮标数据上传成功");
            return result;
        } catch (Exception e) {
            log.error("浮标数据上传失败", e);
            return Map.of("status", "error",
                    "message", "数据上传失败: " + e.getMessage());
        }
    }

    /**
     * GET /actuator/health
     * 健康检查 — 由 Spring Boot Actuator 自动提供
     * BuoyWeatherClient.health() 调用此端点时会返回 Actuator 的 health 结果
     */

    // ─── 辅助方法 ──────────────────────────────────────────

    private Map<String, Object> toMap(BuoyData data, String status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status);
        map.put("id", data.getId());
        map.put("buoyId", data.getBuoyId());
        map.put("buoyName", data.getBuoyName());
        map.put("longitude", data.getLongitude());
        map.put("latitude", data.getLatitude());
        map.put("windSpeed", data.getWindSpeed());
        map.put("windDirection", data.getWindDirection());
        map.put("temperature", data.getTemperature());
        map.put("pressure", data.getPressure());
        map.put("humidity", data.getHumidity());
        map.put("waveHeight", data.getWaveHeight());
        map.put("waterTemperature", data.getWaterTemperature());
        map.put("collectTime", data.getCollectTime() != null
                ? data.getCollectTime().toString() : null);
        return map;
    }

    private Map<String, Object> toBriefMap(BuoyData data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", data.getId());
        map.put("buoyId", data.getBuoyId());
        map.put("buoyName", data.getBuoyName());
        map.put("longitude", data.getLongitude());
        map.put("latitude", data.getLatitude());
        map.put("collectTime", data.getCollectTime() != null
                ? data.getCollectTime().toString() : null);
        return map;
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
