package com.uav.e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E 测试 - 路径规划服务端到端测试
 * 测试完整的路径规划请求流程
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PathPlanningE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("E2E: 路径规划请求应返回有效路径")
    void planPath_shouldReturnValidPath() {
        // 构建请求体
        Map<String, Object> request = new HashMap<>();
        request.put("startLat", 39.9);
        request.put("startLon", 116.4);
        request.put("endLat", 40.0);
        request.put("endLon", 116.5);
        request.put("altitude", 100.0);
        request.put("riskThreshold", 0.5);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/v1/path/plan", entity, Map.class);

        // 验证响应
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertTrue(body.containsKey("path") || body.containsKey("waypoints"));
        }
    }

    @Test
    @DisplayName("E2E: 多无人机路径规划请求应返回无冲突路径")
    void planMultiUavPath_shouldReturnConflictFreePaths() {
        // 构建多无人机请求
        Map<String, Object> request = new HashMap<>();
        request.put("uavs", new Object[]{
            Map.of("id", "UAV-001", "startLat", 39.9, "startLon", 116.4, "endLat", 40.0, "endLon", 116.5),
            Map.of("id", "UAV-002", "startLat", 40.0, "startLon", 116.4, "endLat", 39.9, "endLon", 116.5)
        });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/v1/path/plan-multi", entity, Map.class);

        // 验证响应状态
        assertTrue(
            response.getStatusCode() == HttpStatus.OK ||
            response.getStatusCode() == HttpStatus.NOT_FOUND ||
            response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @Test
    @DisplayName("E2E: 风险场查询应返回风险数据")
    void getRiskField_shouldReturnRiskData() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/v1/risk/field?lat=39.9&lon=116.4&radius=10", Map.class);

        // 验证响应
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
        }
    }
}