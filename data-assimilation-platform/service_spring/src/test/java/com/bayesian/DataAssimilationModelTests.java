package com.bayesian;

import com.bayesian.dto.request.AssimilationRequest;
import com.bayesian.dto.request.BatchRequest;
import com.bayesian.dto.response.AssimilationResponse;
import com.bayesian.dto.response.StatusResponse;
import com.bayesian.exception.AssimilationException;
import com.bayesian.exception.DegradedModeException;
import com.bayesian.model.Job;
import com.bayesian.service.AlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("数据同化平台模型与DTO测试")
class DataAssimilationModelTests {

    @Test
    @DisplayName("AssimilationRequest DTO测试")
    void testAssimilationRequest() {
        AssimilationRequest request = new AssimilationRequest();
        request.setMethod("3dvar");
        request.setDomainSize("1000,1000,100");
        request.setResolution(50.0);
        request.setBackground(new HashMap<>());
        request.setObservations(new HashMap<>());
        request.setConfig(new HashMap<>());

        assertEquals("3dvar", request.getMethod());
        assertEquals("1000,1000,100", request.getDomainSize());
        assertEquals(50.0, request.getResolution());
        assertNotNull(request.getBackground());
        assertNotNull(request.getObservations());
        assertNotNull(request.getConfig());
    }

    @Test
    @DisplayName("BatchRequest DTO测试")
    void testBatchRequest() {
        BatchRequest request = new BatchRequest();
        request.setBatchId("batch-001");
        request.setRequests(new java.util.ArrayList<>());
        assertEquals("batch-001", request.getBatchId());
        assertNotNull(request.getRequests());
    }

    @Test
    @DisplayName("AssimilationResponse DTO测试")
    void testAssimilationResponse() {
        AssimilationResponse response = new AssimilationResponse();
        response.setStatus("success");
        response.setAnalysis(Map.of("mean", 10.0));
        response.setVariance(Map.of("var", 2.0));
        response.setMetrics(Map.of("time", 1.5));
        response.setMessage("同化完成");

        assertEquals("success", response.getStatus());
        assertEquals(Map.of("mean", 10.0), response.getAnalysis());
        assertEquals(Map.of("var", 2.0), response.getVariance());
        assertEquals(Map.of("time", 1.5), response.getMetrics());
        assertEquals("同化完成", response.getMessage());
    }

    @Test
    @DisplayName("StatusResponse DTO测试")
    void testStatusResponse() {
        StatusResponse response = new StatusResponse("running", "1.0.0");
        response.setGpuAvailable(true);
        response.setActiveJobs(3);

        assertEquals("running", response.getStatus());
        assertEquals("1.0.0", response.getVersion());
        assertTrue(response.isGpuAvailable());
        assertEquals(3, response.getActiveJobs());

        StatusResponse emptyResponse = new StatusResponse();
        assertNull(emptyResponse.getStatus());
    }

    @Test
    @DisplayName("Job实体测试")
    void testJobEntity() {
        Job job = new Job();
        job.setJobId("JOB-001");
        job.setType("assimilation");
        job.setStatus("running");
        job.setProgress(50);
        job.setStartedAt(System.currentTimeMillis());
        job.setResult(Map.of("analysis", "data"));

        assertEquals("JOB-001", job.getJobId());
        assertEquals("assimilation", job.getType());
        assertEquals("running", job.getStatus());
        assertEquals(50, job.getProgress());
        assertNotNull(job.getResult());
    }

    @Test
    @DisplayName("AssimilationException测试")
    void testAssimilationException() {
        AssimilationException ex = new AssimilationException("同化计算失败");
        assertEquals("同化计算失败", ex.getMessage());

        Throwable cause = new RuntimeException("根源异常");
        AssimilationException exWithCause = new AssimilationException("计算异常", cause);
        assertEquals("计算异常", exWithCause.getMessage());
        assertSame(cause, exWithCause.getCause());
    }

    @Test
    @DisplayName("DegradedModeException测试")
    void testDegradedModeException() {
        DegradedModeException ex = new DegradedModeException("降级模式已启用");
        assertEquals("降级模式已启用", ex.getMessage());

        DegradedModeException exWithCause = new DegradedModeException("服务降级", new RuntimeException("连接超时"));
        assertEquals("服务降级", exWithCause.getMessage());
        assertNotNull(exWithCause.getCause());
    }

    @Test
    @DisplayName("AlertService测试")
    void testAlertService() {
        AlertService alertService = new AlertService();
        assertDoesNotThrow(() -> alertService.sendAlert("CRITICAL", "系统异常"));
        assertDoesNotThrow(() -> alertService.notifyDegradedMode("wrf-processor"));
        assertDoesNotThrow(() -> alertService.notifyRecovery("wrf-processor"));
    }
}