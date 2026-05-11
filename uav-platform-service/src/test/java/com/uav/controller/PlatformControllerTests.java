package com.uav.controller;

import com.uav.common.feign.DataAssimilationClient;
import com.uav.common.feign.MeteorForecastClient;
import com.uav.common.feign.PathPlanningClient;
import com.uav.common.feign.WrfProcessorClient;
import com.uav.platform.controller.PlatformController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PlatformController 单元测试
 * 测试使用Feign Client重构后的Controller
 */
@DisplayName("PlatformController 单元测试")
class PlatformControllerTests {

    private PlatformController controller;
    private WrfProcessorClient wrfProcessorClient;
    private DataAssimilationClient dataAssimilationClient;
    private MeteorForecastClient meteorForecastClient;
    private PathPlanningClient pathPlanningClient;

    @BeforeEach
    void setUp() {
        // 创建模拟的Feign客户端
        wrfProcessorClient = mock(WrfProcessorClient.class);
        dataAssimilationClient = mock(DataAssimilationClient.class);
        meteorForecastClient = mock(MeteorForecastClient.class);
        pathPlanningClient = mock(PathPlanningClient.class);

        // 使用构造函数注入
        controller = new PlatformController(
                wrfProcessorClient,
                dataAssimilationClient,
                meteorForecastClient,
                pathPlanningClient
        );
    }

    @Test
    @DisplayName("manageTask 返回成功响应")
    void testManageTask() {
        Map<String, Object> request = Map.of("action", "start");
        Map<String, Object> result = controller.manageTask(request);

        assertTrue((Boolean) result.get("success"));
        assertEquals("任务管理成功", result.get("message"));
    }

    @Test
    @DisplayName("getDrones 返回成功响应")
    void testGetDrones() {
        Map<String, Object> result = controller.getDrones();

        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("data"));
    }

    @Test
    @DisplayName("plan 在无 weatherData 时返回错误")
    void testPlanWithoutWeatherData() {
        Map<String, Object> request = Map.of("drones", Map.of(), "tasks", Map.of());
        Map<String, Object> result = controller.plan(request);

        assertFalse((Boolean) result.get("success"));
        assertEquals("气象数据不能为空", result.get("error"));
    }

    @Test
    @DisplayName("healthCheck 返回服务健康状态")
    void testHealthCheck() {
        // 模拟所有服务健康
        when(wrfProcessorClient.health()).thenReturn(Map.of("status", "UP"));
        when(dataAssimilationClient.health()).thenReturn(Map.of("status", "UP"));
        when(meteorForecastClient.health()).thenReturn(Map.of("status", "UP"));
        when(pathPlanningClient.health()).thenReturn(Map.of("status", "UP"));

        Map<String, Object> result = controller.healthCheck();

        assertEquals("UP", result.get("status"));
    }

    @Test
    @DisplayName("plan 综合路径规划成功")
    void testPlanSuccess() {
        // 准备测试数据
        Map<String, Object> weatherData = Map.of("temperature", 25.0);
        Map<String, Object> request = Map.of(
                "drones", Map.of("id", "drone1"),
                "tasks", Map.of("id", "task1"),
                "weatherData", weatherData,
                "obstacles", Map.of(),
                "noFlyZones", Map.of()
        );

        // 模拟服务响应
        when(wrfProcessorClient.parseWrfData(any())).thenReturn(Map.of("success", true, "data", Map.of()));
        when(dataAssimilationClient.executeAssimilation(any())).thenReturn(Map.of("success", true, "data", Map.of()));
        when(meteorForecastClient.getDetailedForecast(any())).thenReturn(Map.of("success", true, "data", Map.of()));
        when(pathPlanningClient.planFull(any())).thenReturn(Map.of("success", true, "data", Map.of()));

        Map<String, Object> result = controller.plan(request);

        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("data"));
    }

    @Test
    @DisplayName("getWeather 返回天气数据")
    void testGetWeather() {
        when(wrfProcessorClient.getWrfDataDetail(anyLong())).thenReturn(Map.of("success", true, "data", Map.of()));

        Map<String, Object> result = controller.getWeather("1");

        assertTrue((Boolean) result.get("success"));
    }
}
