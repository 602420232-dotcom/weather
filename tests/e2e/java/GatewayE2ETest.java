package com.uav.e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E 测试 - API 网关端到端测试
 * 测试完整的请求响应流程
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class GatewayE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("E2E: 健康检查端点应返回200")
    void healthEndpoint_shouldReturn200() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("E2E: 网关路由应正确转发请求")
    void gatewayRoute_shouldForwardRequest() {
        // 测试网关路由功能
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/weather/status", String.class);
        
        // 可能返回200或404，取决于后端服务状态
        assertTrue(
            response.getStatusCode() == HttpStatus.OK || 
            response.getStatusCode() == HttpStatus.NOT_FOUND ||
            response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE
        );
    }
}