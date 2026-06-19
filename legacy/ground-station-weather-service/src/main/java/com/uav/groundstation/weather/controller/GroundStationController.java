package com.uav.groundstation.weather.controller;

import com.uav.groundstation.weather.model.GroundStationData;
import com.uav.groundstation.weather.repository.GroundStationDataRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.annotation.*;

/**
 * 地面气象站数据 Controller
 * 端点路径与 GroundStationWeatherClient Feign 接口保持一致
 */
@Slf4j
@RestController
@RequestMapping("/api/ground-station")
@RequiredArgsConstructor
@Tag(name = "地面气象站", description = "地面气象站数据查询与管理接口")
public class GroundStationController {

    private final GroundStationDataRepository repository;

    /**
     * GET /api/ground-station/data?stationId=xxx
     * 获取地面气象站数据
     */
    @GetMapping("/data")
    @Operation(summary = "获取地面气象站数据")
    public ResponseEntity<Map<String, Object>> getStationData(
            @RequestParam(value = "stationId", required = false) String stationId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (stationId != null && !stationId.isBlank()) {
            repository.findLatestByStationId(stationId).ifPresentOrElse(
                    data -> {
                        result.put("success", true);
                        result.put("data", data);
                    },
                    () -> {
                        result.put("success", false);
                        result.put("message", "站点数据不存在: " + stationId);
                    }
            );
        } else {
            List<GroundStationData> all = repository.findAll();
            result.put("success", true);
            result.put("total", all.size());
            result.put("data", all);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ground-station/list?page=1&size=10
     * 获取地面气象站分页列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取地面气象站列表")
    public ResponseEntity<Map<String, Object>> listStations(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Page<GroundStationData> pageResult = repository.findAll(PageRequest.of(page - 1, size));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", pageResult.getTotalPages());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ground-station/{id}
     * 获取气象站详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取气象站详情")
    public ResponseEntity<Map<String, Object>> getStationDetail(@PathVariable("id") Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        repository.findById(Objects.requireNonNull(id, "id must not be null")).ifPresentOrElse(
                data -> {
                    result.put("success", true);
                    result.put("data", data);
                },
                () -> {
                    result.put("success", false);
                    result.put("message", "气象站数据不存在: id=" + id);
                }
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ground-station/realtime?stationId=xxx
     * 获取气象站实时数据(最新观测)
     */
    @GetMapping("/realtime")
    @Operation(summary = "获取气象站实时数据")
    public ResponseEntity<Map<String, Object>> getRealtimeData(
            @RequestParam(value = "stationId") String stationId) {
        Map<String, Object> result = new LinkedHashMap<>();
        repository.findLatestByStationId(stationId).ifPresentOrElse(
                data -> {
                    result.put("success", true);
                    result.put("data", data);
                    result.put("collectTime", data.getCollectTime());
                },
                () -> {
                    result.put("success", false);
                    result.put("message", "暂无实时数据: " + stationId);
                }
        );
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ground-station/upload
     * 上传气象站数据
     */
    @PostMapping("/upload")
    @Operation(summary = "上传气象站数据")
    public ResponseEntity<Map<String, Object>> uploadStationData(@RequestBody Map<String, Object> request) {
        try {
            GroundStationData data = GroundStationData.builder()
                    .stationId((String) request.getOrDefault("stationId", "UNKNOWN"))
                    .stationName((String) request.getOrDefault("stationName", "未命名站点"))
                    .longitude(parseDouble(request.get("longitude")))
                    .latitude(parseDouble(request.get("latitude")))
                    .altitude(parseDouble(request.get("altitude")))
                    .windSpeed(parseDouble(request.get("windSpeed")))
                    .windDirection(parseDouble(request.get("windDirection")))
                    .temperature(parseDouble(request.get("temperature")))
                    .pressure(parseDouble(request.get("pressure")))
                    .humidity(parseDouble(request.get("humidity")))
                    .precipitation(parseDouble(request.get("precipitation")))
                    .visibility(parseDouble(request.get("visibility")))
                    .collectTime(parseDateTime(request.get("collectTime")))
                    .build();

            GroundStationData saved = repository.save(Objects.requireNonNull(data, "data must not be null"));
            log.info("地面气象站数据上传成功: stationId={}, id={}", saved.getStationId(), saved.getId());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "数据上传成功");
            result.put("id", saved.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("地面气象站数据上传失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "数据上传失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    private Double parseDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return LocalDateTime.now();
        if (value instanceof LocalDateTime dt) return dt;
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
