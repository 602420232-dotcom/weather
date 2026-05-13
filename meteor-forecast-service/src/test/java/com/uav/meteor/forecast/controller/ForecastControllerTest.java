package com.uav.meteor.forecast.controller;

import com.uav.common.dto.ForecastRequest;
import com.uav.common.feign.PythonScriptInvoker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForecastController 气象预报控制器测试")
class ForecastControllerTest {

    @Mock
    private PythonScriptInvoker pythonScriptInvoker;

    @InjectMocks
    private ForecastController forecastController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(Objects.requireNonNull(forecastController), "pythonScriptPath", "meteor_forecast.py");
    }

    private ForecastRequest createValidRequest() {
        ForecastRequest request = new ForecastRequest();
        request.setMethod("lstm");
        request.setData(Map.of("temperature", 25.0, "humidity", 60));
        request.setConfig(Map.of("epochs", 100));
        return request;
    }

    @Test
    @DisplayName("测试单点预测")
    void testPredict() {
        ForecastRequest request = createValidRequest();
        when(pythonScriptInvoker.executeAsMap(any(), any(), any()))
                .thenReturn(Map.of("success", true, "forecast", "{}"));
        Map<String, Object> result = forecastController.predict(request);
        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.get("success"));
    }

    @Test
    @DisplayName("测试数据订正")
    void testCorrect() {
        ForecastRequest request = createValidRequest();
        when(pythonScriptInvoker.executeAsMap(any(), any(), any()))
                .thenReturn(Map.of("success", true, "corrected", "{}"));
        Map<String, Object> result = forecastController.correct(request);
        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.get("success"));
    }

    @Test
    @DisplayName("测试模型列表")
    void testGetModels() {
        Map<String, Object> result = forecastController.getModels();
        assertNotNull(result);
        assertEquals(Boolean.TRUE, result.get("success"));
    }
}
