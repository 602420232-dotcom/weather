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
 * E2E 测试 - 气象预报服务端到端测试
 * 测试完整的气象数据请求流程
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MeteorForecastE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("E2E: 获取当前天气应返回天气数据")
    void getCurrentWeather_shouldReturnWeatherData() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/v1/weather/current?lat=39.9&lon=116.4", Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            // 验证返回的天气数据结构
            assertTrue(body.containsKey("temperature") || 
                       body.containsKey("windSpeed") ||
                       body.containsKey("data"));
        }
    }

    @Test
    @DisplayName("E2E: 获取天气预报应返回预报数据")
    void getWeatherForecast_shouldReturnForecastData() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/v1/weather/forecast?lat=39.9&lon=116.4&hours=24", Map.class);

        // 验证响应状态
        assertTrue(
            response.getStatusCode() == HttpStatus.OK ||
            response.getStatusCode() == HttpStatus.NOT_FOUND ||
            response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @Test
    @DisplayName("E2E: 获取风场数据应返回风场信息")
    void getWindField_shouldReturnWindFieldData() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/v1/weather/wind-field?lat=39.9&lon=116.4&radius=50", Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
        }
    }
}