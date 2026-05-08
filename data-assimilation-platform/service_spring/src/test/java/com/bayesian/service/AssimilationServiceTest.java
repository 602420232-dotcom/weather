package com.bayesian.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssimilationService 单元测试")
class AssimilationServiceTest {

    @Mock
    private PythonService pythonService;

    @InjectMocks
    private AssimilationService service;

    @Nested
    @DisplayName("executeAssimilation")
    class ExecuteAssimilationTests {
        @Test
        @DisplayName("成功执行返回completed状态")
        void shouldReturnCompletedOnSuccess() {
            Map<String, Object> request = Map.of("algorithm", "3dvar");
            when(pythonService.executeAssimilation(any())).thenReturn("{\"result\":\"ok\"}");

            Map<String, Object> result = service.executeAssimilation(request);

            assertEquals("completed", result.get("status"));
            assertNotNull(result.get("jobId"));
            assertEquals("{\"result\":\"ok\"}", result.get("result"));
        }

        @Test
        @DisplayName("Python服务失败返回failed状态")
        void shouldReturnFailedOnPythonError() {
            Map<String, Object> request = Map.of("algorithm", "4dvar");
            when(pythonService.executeAssimilation(any())).thenThrow(new RuntimeException("Python error"));

            Map<String, Object> result = service.executeAssimilation(request);

            assertEquals("failed", result.get("status"));
            assertEquals("同化处理失败", result.get("error"));
        }

        @Test
        @DisplayName("每次执行生成唯一jobId")
        void shouldGenerateUniqueJobId() {
            when(pythonService.executeAssimilation(any())).thenReturn("ok");
            Map<String, Object> r1 = service.executeAssimilation(Map.of());
            Map<String, Object> r2 = service.executeAssimilation(Map.of());
            assertNotEquals(r1.get("jobId"), r2.get("jobId"));
        }
    }

    @Nested
    @DisplayName("getJobStatus")
    class GetJobStatusTests {
        @Test
        @DisplayName("不存在的jobId返回not_found")
        void shouldReturnNotFoundForMissingJobId() {
            Object status = service.getJobStatus("nonexistent");
            Map<String, Object> statusMap = (Map<String, Object>) status;
            assertEquals("not_found", statusMap.get("status"));
        }

        @Test
        @DisplayName("已完成的job返回completed状态")
        void shouldReturnCompletedStatus() {
            Map<String, Object> request = Map.of("algorithm", "3dvar");
            when(pythonService.executeAssimilation(any())).thenReturn("ok");
            Map<String, Object> result = service.executeAssimilation(request);
            String jobId = (String) result.get("jobId");

            Object status = service.getJobStatus(jobId);
            Map<String, Object> statusMap = (Map<String, Object>) status;
            assertEquals("completed", statusMap.get("status"));
        }

        @Test
        @DisplayName("失败的job保留failed状态")
        void shouldRetainFailedStatus() {
            when(pythonService.executeAssimilation(any())).thenThrow(new RuntimeException("fail"));
            Map<String, Object> result = service.executeAssimilation(Map.of());
            String jobId = (String) result.get("jobId");

            Object status = service.getJobStatus(jobId);
            Map<String, Object> statusMap = (Map<String, Object>) status;
            assertEquals("failed", statusMap.get("status"));
        }
    }
}
