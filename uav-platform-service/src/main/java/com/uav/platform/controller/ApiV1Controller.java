package com.uav.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
public class ApiV1Controller {

    // ===== Drones =====
    @GetMapping("/api/v1/drones")
    public ResponseEntity<Map<String, Object>> getDrones() {
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "data", List.of(
                Map.of("id", "D-001", "name", "猎鹰-1号", "model", "DJI-M300", "type", "多旋翼",
                    "maxPayload", 2.5, "maxFlightTime", 30, "maxSpeed", 15,
                    "status", "在线", "battery", 92),
                Map.of("id", "D-002", "name", "天鹰-2号", "model", "DJI-M300", "type", "多旋翼",
                    "maxPayload", 3.0, "maxFlightTime", 35, "maxSpeed", 12,
                    "status", "执行任务", "battery", 78)
            ),
            "message", "success"
        ));
    }

    @PostMapping("/api/v1/drones")
    public ResponseEntity<Map<String, Object>> createDrone(@RequestBody Map<String, Object> drone) {
        return ResponseEntity.ok(Map.of("code", 200, "data", drone, "message", "created"));
    }

    @PutMapping("/api/v1/drones/{id}")
    public ResponseEntity<Map<String, Object>> updateDrone(@PathVariable String id, @RequestBody Map<String, Object> drone) {
        return ResponseEntity.ok(Map.of("code", 200, "data", drone, "message", "updated"));
    }

    @DeleteMapping("/api/v1/drones/{id}")
    public ResponseEntity<Map<String, Object>> deleteDrone(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("code", 200, "message", "deleted"));
    }

    // ===== Tasks =====
    @GetMapping("/api/v1/tasks")
    public ResponseEntity<Map<String, Object>> getTasks() {
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "data", List.of(
                Map.of("id", "T001", "name", "市区物流配送", "type", "delivery",
                    "status", "执行中", "priority", "高",
                    "waypoints", List.of(Map.of("lat", 39.90, "lng", 116.40, "order", 1)),
                    "createdAt", LocalDateTime.now().minusHours(2).toString()),
                Map.of("id", "T002", "name", "电力线路巡检-A区", "type", "inspection",
                    "status", "已分配", "priority", "中",
                    "waypoints", List.of(Map.of("lat", 39.92, "lng", 116.42, "order", 1)),
                    "createdAt", LocalDateTime.now().minusHours(1).toString())
            ),
            "message", "success"
        ));
    }

    @PostMapping("/api/v1/tasks")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Map<String, Object> task) {
        return ResponseEntity.ok(Map.of("code", 200, "data", task, "message", "created"));
    }

    @PutMapping("/api/v1/tasks/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(@PathVariable String id, @RequestBody Map<String, Object> task) {
        return ResponseEntity.ok(Map.of("code", 200, "data", task, "message", "updated"));
    }

    @DeleteMapping("/api/v1/tasks/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("code", 200, "message", "deleted"));
    }

    // ===== Auth =====
    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> loginReq) {
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "token", "demo-token-" + UUID.randomUUID(),
            "refreshToken", "demo-refresh-" + UUID.randomUUID(),
            "user", Map.of("id", "U001", "username", loginReq.getOrDefault("username", "admin"), "role", "admin"),
            "message", "login success"
        ));
    }

    @GetMapping("/api/v1/auth/me")
    public ResponseEntity<Map<String, Object>> currentUser() {
        return ResponseEntity.ok(Map.of(
            "id", "U001", "username", "admin", "role", "admin", "name", "管理员"
        ));
    }

    // ===== Weather Sources =====
    @GetMapping("/api/weather/sources")
    public ResponseEntity<Map<String, Object>> weatherSources() {
        return ResponseEntity.ok(Map.of("code", 200, "data", List.of(), "message", "暂无数据源"));
    }

    // ===== WRF Data =====
    @GetMapping("/api/wrf/data")
    public ResponseEntity<Map<String, Object>> wrfData() {
        return ResponseEntity.ok(Map.of("code", 200, "data", Map.of(), "message", "WRF服务暂不可用"));
    }

    // ===== Forecast =====
    @GetMapping("/api/forecast/models")
    public ResponseEntity<Map<String, Object>> forecastModels() {
        return ResponseEntity.ok(Map.of("code", 200, "data", List.of(), "message", "暂无可用模型"));
    }

    // ===== Planning =====
    @PostMapping("/api/planning/full")
    public ResponseEntity<Map<String, Object>> fullPlanning(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("message", "路径规划服务暂不可用"), "message", "success"));
    }

    @GetMapping("/api/planning/full")
    public ResponseEntity<Map<String, Object>> fullPlanningStatus() {
        return ResponseEntity.ok(Map.of("code", 200, "message", "路径规划服务暂不可用"));
    }

    // ===== Health =====
    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", LocalDateTime.now().toString()));
    }
}
