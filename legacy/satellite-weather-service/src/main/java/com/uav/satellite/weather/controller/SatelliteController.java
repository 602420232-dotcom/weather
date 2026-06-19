package com.uav.satellite.weather.controller;

import com.uav.satellite.weather.model.SatelliteImage;
import com.uav.satellite.weather.repository.SatelliteImageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.annotation.*;

/**
 * 卫星气象数据 Controller
 * 端点路径与 SatelliteWeatherClient Feign 接口保持一致
 */
@Slf4j
@RestController
@RequestMapping("/api/satellite")
@RequiredArgsConstructor
@Tag(name = "卫星气象", description = "卫星云图数据查询与管理接口")
public class SatelliteController {

    private final SatelliteImageRepository repository;

    /**
     * GET /api/satellite/cloud?region=CHINA&channel=IR
     * 获取卫星云图数据
     */
    @GetMapping("/cloud")
    @Operation(summary = "获取卫星云图数据")
    public ResponseEntity<Map<String, Object>> getCloudImage(
            @RequestParam(value = "region", defaultValue = "CHINA") String region,
            @RequestParam(value = "channel", defaultValue = "IR") String channel) {
        Map<String, Object> result = new LinkedHashMap<>();
        repository.findLatestByRegionAndChannel(region, channel).ifPresentOrElse(
                image -> {
                    result.put("success", true);
                    result.put("data", image);
                },
                () -> {
                    result.put("success", false);
                    result.put("message", "暂无云图数据: region=" + region + ", channel=" + channel);
                }
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/satellite/list?page=1&size=10
     * 获取卫星云图列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取卫星云图列表")
    public ResponseEntity<Map<String, Object>> listSatelliteImages(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Page<SatelliteImage> pageResult = repository.findAll(PageRequest.of(page - 1, size));
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
     * GET /api/satellite/{id}
     * 获取卫星云图详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取卫星云图详情")
    public ResponseEntity<Map<String, Object>> getSatelliteImageDetail(@PathVariable("id") Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        repository.findById(Objects.requireNonNull(id, "id must not be null")).ifPresentOrElse(
                image -> {
                    result.put("success", true);
                    result.put("data", image);
                },
                () -> {
                    result.put("success", false);
                    result.put("message", "云图数据不存在: id=" + id);
                }
        );
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/satellite/upload
     * 上传卫星数据
     */
    @PostMapping("/upload")
    @Operation(summary = "上传卫星数据")
    public ResponseEntity<Map<String, Object>> uploadSatelliteData(@RequestBody Map<String, Object> request) {
        try {
            SatelliteImage image = SatelliteImage.builder()
                    .satelliteId((String) request.getOrDefault("satelliteId", "UNKNOWN"))
                    .satelliteName((String) request.getOrDefault("satelliteName", "未命名卫星"))
                    .region((String) request.getOrDefault("region", "GLOBAL"))
                    .channel((String) request.getOrDefault("channel", "IR"))
                    .imageUrl((String) request.get("imageUrl"))
                    .topLeftLon(parseDouble(request.get("topLeftLon")))
                    .topLeftLat(parseDouble(request.get("topLeftLat")))
                    .bottomRightLon(parseDouble(request.get("bottomRightLon")))
                    .bottomRightLat(parseDouble(request.get("bottomRightLat")))
                    .resolution(parseDouble(request.get("resolution")))
                    .captureTime(parseDateTime(request.get("captureTime")))
                    .build();

            SatelliteImage saved = repository.save(Objects.requireNonNull(image, "image must not be null"));
            log.info("卫星云图数据上传成功: satelliteId={}, region={}, channel={}, id={}",
                    saved.getSatelliteId(), saved.getRegion(), saved.getChannel(), saved.getId());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "数据上传成功");
            result.put("id", saved.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("卫星云图数据上传失败", e);
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
