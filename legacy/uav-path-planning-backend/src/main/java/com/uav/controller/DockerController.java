package com.uav.controller;

import com.uav.service.DockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Docker 容器管理接口
 *
 * 提供容器列表、详情、启停重启、日志、资源统计、主机概览等 REST API。
 * 路径前缀与前端 api/docker.js 中的 /v1/docker 一致。
 */
@RestController
@RequestMapping("/api/v1/docker")
public class DockerController {

    private final DockerService dockerService;

    public DockerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    /**
     * GET /api/v1/docker/containers
     * 获取所有容器列表（含已停止容器）
     */
    @GetMapping("/containers")
    public ResponseEntity<Map<String, Object>> getContainers() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        try {
            List<Map<String, Object>> containers = dockerService.listContainers(true);
            result.put("data", containers);
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
            result.put("data", Collections.emptyList());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/docker/containers/{id}
     * 获取单个容器详情
     */
    @GetMapping("/containers/{id}")
    public ResponseEntity<Map<String, Object>> getContainerById(@PathVariable String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        try {
            Map<String, Object> container = dockerService.inspectContainer(id);
            result.put("data", container);
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
            result.put("data", null);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/docker/host/stats
     * 获取 Docker 主机系统资源统计
     */
    @GetMapping("/host/stats")
    public ResponseEntity<Map<String, Object>> getHostStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        try {
            Map<String, Object> stats = dockerService.getHostStats();
            result.put("data", stats);
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
            result.put("data", null);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/docker/containers/{id}/restart
     * 重启容器
     */
    @PostMapping("/containers/{id}/restart")
    public ResponseEntity<Map<String, Object>> restartContainer(@PathVariable String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            dockerService.restartContainer(id);
            result.put("code", 0);
            result.put("message", "容器重启成功");
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/docker/containers/{id}/stop
     * 停止容器
     */
    @PostMapping("/containers/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopContainer(@PathVariable String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            dockerService.stopContainer(id);
            result.put("code", 0);
            result.put("message", "容器已停止");
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/docker/containers/{id}/start
     * 启动容器
     */
    @PostMapping("/containers/{id}/start")
    public ResponseEntity<Map<String, Object>> startContainer(@PathVariable String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            dockerService.startContainer(id);
            result.put("code", 0);
            result.put("message", "容器已启动");
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/docker/containers/{id}/logs
     * 获取容器日志
     * @param tail  日志行数（默认 100）
     */
    @GetMapping("/containers/{id}/logs")
    public ResponseEntity<Map<String, Object>> getContainerLogs(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int tail) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        try {
            String logs = dockerService.getContainerLogs(id, tail);
            result.put("data", logs);
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
            result.put("data", "");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/docker/containers/{id}/stats
     * 获取容器资源使用统计
     */
    @GetMapping("/containers/{id}/stats")
    public ResponseEntity<Map<String, Object>> getContainerStats(@PathVariable String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        try {
            Map<String, Object> stats = dockerService.getContainerStats(id);
            result.put("data", stats);
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
            result.put("data", null);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/v1/docker/containers/prune
     * 清理已停止的容器
     */
    @DeleteMapping("/containers/prune")
    public ResponseEntity<Map<String, Object>> pruneContainers() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        try {
            Map<String, Object> pruneResult = dockerService.pruneContainers();
            result.put("data", pruneResult);
        } catch (Exception e) {
            result.put("code", -1);
            result.put("message", e.getMessage());
            result.put("data", null);
        }
        return ResponseEntity.ok(result);
    }
}
