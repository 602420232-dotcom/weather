package com.uav.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessException 单元测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("创建带code和message的异常")
    void testCreateWithCodeAndMessage() {
        BusinessException ex = new BusinessException("VALIDATION_ERROR", "参数校验失败");
        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertEquals("参数校验失败", ex.getMessage());
    }

    @Test
    @DisplayName("创建带cause的异常")
    void testCreateWithCause() {
        Throwable cause = new RuntimeException("根源异常");
        BusinessException ex = new BusinessException("DB_ERROR", "数据库错误", cause);
        assertEquals("DB_ERROR", ex.getCode());
        assertEquals("数据库错误", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("创建null code的异常")
    void testCreateWithNullCode() {
        BusinessException ex = new BusinessException(null, "未知错误");
        assertNull(ex.getCode());
    }
}

@DisplayName("DataNotFoundException 单元测试")
class DataNotFoundExceptionTest {

    @Test
    @DisplayName("创建数据不存在异常")
    void testCreateWithEntityAndId() {
        DataNotFoundException ex = new DataNotFoundException("User", 42L);
        assertEquals("User", ex.getEntity());
        assertEquals(42L, ex.getId());
        assertTrue(ex.getMessage().contains("User"));
        assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    @DisplayName("创建带cause的数据不存在异常")
    void testCreateWithCause() {
        Throwable cause = new RuntimeException("查询失败");
        DataNotFoundException ex = new DataNotFoundException("Drone", "UAV-001", cause);
        assertEquals("Drone", ex.getEntity());
        assertEquals("UAV-001", ex.getId());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("使用Integer类型id")
    void testCreateWithIntegerId() {
        DataNotFoundException ex = new DataNotFoundException("Task", 100);
        assertEquals(100, ex.getId());
    }
}

@DisplayName("ServiceUnavailableException 单元测试")
class ServiceUnavailableExceptionTest {

    @Test
    @DisplayName("创建服务不可用异常")
    void testCreateWithServiceName() {
        ServiceUnavailableException ex = new ServiceUnavailableException("wrf-processor", "WRF服务暂时不可用");
        assertEquals("wrf-processor", ex.getServiceName());
        assertEquals("WRF服务暂时不可用", ex.getMessage());
    }

    @Test
    @DisplayName("创建带cause的服务不可用异常")
    void testCreateWithCause() {
        Throwable cause = new RuntimeException("连接超时");
        ServiceUnavailableException ex = new ServiceUnavailableException("data-assimilation", "同化服务不可用", cause);
        assertEquals("data-assimilation", ex.getServiceName());
        assertSame(cause, ex.getCause());
    }
}

@DisplayName("PythonExecutionException 单元测试")
class PythonExecutionExceptionTest {

    @Test
    @DisplayName("创建Python执行异常")
    void testCreateWithScriptName() {
        PythonExecutionException ex = new PythonExecutionException("meteor_forecast.py", "脚本执行超时");
        assertEquals("meteor_forecast.py", ex.getScriptName());
        assertEquals("脚本执行超时", ex.getMessage());
    }

    @Test
    @DisplayName("创建带cause的Python执行异常")
    void testCreateWithCause() {
        Throwable cause = new RuntimeException("内存不足");
        PythonExecutionException ex = new PythonExecutionException("assimilation.py", "执行失败", cause);
        assertEquals("assimilation.py", ex.getScriptName());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("创建空脚本名的异常")
    void testCreateWithEmptyScriptName() {
        PythonExecutionException ex = new PythonExecutionException("", "未知脚本错误");
        assertEquals("", ex.getScriptName());
    }
}