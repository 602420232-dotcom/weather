package com.uav.wrf.processor.service;

import com.uav.common.script.PythonScriptInvoker;
import com.uav.wrf.processor.entity.WrfDataFile;
import com.uav.wrf.processor.repository.WrfDataFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WrfDataService 单元测试")
@SuppressWarnings("null")
class WrfDataServiceTest {

    @Mock
    private WrfDataFileRepository repository;

    /**
     * ★ 2026-09-28：C2 之后三个诊断端点改为「调 Python 解析器拿真实 risk 块」，
     * 因此 WrfDataService 的构造器多了一个依赖（PythonScriptInvoker）。
     * 这里把它 mock 掉：**单测只验管道与分档，不验物理**（物理由真实链路的
     * {@code WrfPlatformIntegrationTest} 负责）。
     */
    @Mock
    private PythonScriptInvoker pythonScriptInvoker;

    private WrfDataService service;

    @BeforeEach
    void setUp() {
        service = new WrfDataService(repository, pythonScriptInvoker);
        ReflectionTestUtils.setField(service, "pythonScriptPath", "wrf_processor.py");
    }

    /** 构造一份与 wrf_processor.py 输出同形的 risk 块。 */
    private Map<String, Object> riskResult(double tkeMax, double visMin, double visMean,
                                           double capeMax, double cinMin, double lclMean) {
        return Map.of(
                "success", true,
                "risk", Map.of(
                        "turbulence", Map.of(
                                "tke_mean", tkeMax / 2,
                                "tke_max", tkeMax,
                                "tke_min", 0.0,
                                "dissipation_rate", 0.02),
                        "visibility", Map.of(
                                "visibility_min_m", visMin,
                                "visibility_mean_m", visMean,
                                "extinction_coefficient_mean", 0.4),
                        "convection", Map.of(
                                "cape_max", capeMax,
                                "cape_mean", capeMax / 10,
                                "cin_min", cinMin,
                                "lcl_mean_m", lclMean,
                                "lfc_mean_m", 1200.0)));
    }

    private WrfDataFile createTestFile() {
        return WrfDataFile.builder()
            .id(1L).fileId("wrf_test").fileName("test.nc")
            .filePath("./data/test.nc").fileSize(1024L).height(100)
            .timeSteps(24).variables("temperature,humidity,wind_speed")
            .status("UPLOADED").createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("通过fileId查找文件")
    void testFindByFileId() {
        when(repository.findByFileId("wrf_test")).thenReturn(Optional.of(createTestFile()));
        Optional<WrfDataFile> result = service.findByFileId("wrf_test");
        assertTrue(result.isPresent());
        assertEquals("test.nc", result.get().getFileName());
    }

    @Test
    @DisplayName("通过ID查找文件")
    void testFindById() {
        when(repository.findById(1L)).thenReturn(Optional.of(createTestFile()));
        Optional<WrfDataFile> result = service.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("wrf_test", result.get().getFileId());
    }

    @Test
    @DisplayName("获取所有文件")
    void testFindAll() {
        Page<WrfDataFile> page = new PageImpl<>(List.of(createTestFile()));
        when(repository.findAllByOrderByCreatedAtDesc(any(PageRequest.class))).thenReturn(page);
        Page<WrfDataFile> result = service.findAll(0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("创建WRF数据文件")
    void testCreateWrfDataFile() {
        when(repository.save(any(WrfDataFile.class))).thenReturn(createTestFile());
        WrfDataFile result = service.createWrfDataFile("test.nc", "./data/test.nc", 1024L);
        assertNotNull(result);
        assertEquals("test.nc", result.getFileName());
    }

    @Test
    @DisplayName("获取湍流数据")
    @SuppressWarnings("unchecked")
    void testGetTurbulence() {
        when(repository.findByFileId("wrf_test")).thenReturn(Optional.of(createTestFile()));
        when(pythonScriptInvoker.executeAsMap(anyString(), anyString(), anyMap()))
                .thenReturn(riskResult(2.0, 9000.0, 12000.0, 1200.0, -30.0, 1500.0));
        Map<String, Object> result = service.getTurbulence("wrf_test");
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("data"));
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        // 判据用纯函数当口径：tke_max=2.0 落在 [1.5, 3.0) ⇒ MODERATE
        assertEquals("MODERATE", data.get("turbulence_intensity"));
        assertEquals("MODERATE", WrfRiskMetrics.tkeIntensity(2.0));
        // 回显的 tke_max 必须就是解析器给的数（不是常量）
        assertEquals(2.0, ((Number) data.get("tke_max")).doubleValue(), 1e-9);
    }

    @Test
    @DisplayName("获取能见度数据")
    @SuppressWarnings("unchecked")
    void testGetVisibility() {
        when(repository.findByFileId("wrf_test")).thenReturn(Optional.of(createTestFile()));
        when(pythonScriptInvoker.executeAsMap(anyString(), anyString(), anyMap()))
                .thenReturn(riskResult(2.0, 6000.0, 12000.0, 1200.0, -30.0, 1500.0));
        Map<String, Object> result = service.getVisibility("wrf_test");
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("data"));
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        // 分档看**域最小**（6000 m ∈ [4800, 8000) ⇒ MODERATE），头条值是域平均
        assertEquals("MODERATE", data.get("visibility_category"));
        assertEquals(12000.0, ((Number) data.get("visibility_m")).doubleValue(), 1e-3);
    }

    @Test
    @DisplayName("获取闪电风险评估")
    @SuppressWarnings("unchecked")
    void testGetLightningRisk() {
        when(repository.findByFileId("wrf_test")).thenReturn(Optional.of(createTestFile()));
        when(pythonScriptInvoker.executeAsMap(anyString(), anyString(), anyMap()))
                .thenReturn(riskResult(2.0, 9000.0, 12000.0, 1250.0, -50.0, 1750.0));
        Map<String, Object> result = service.getLightningRisk("wrf_test");
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("data"));
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        // 70×1250/2500 + 20×(1−50/200) + 10×(1750−500)/2500 = 35 + 15 + 5 = 55 ⇒ HIGH
        assertEquals(55.0, ((Number) data.get("risk_score")).doubleValue(), 1e-6);
        assertEquals("HIGH", data.get("risk_level"));
    }

    @Test
    @DisplayName("★ 解析器没给 risk 块时必须报错，不能回退到占位常量")
    @SuppressWarnings("unchecked")
    void testDiagnosticsFailLoudlyWithoutRiskBlock() {
        when(repository.findByFileId("wrf_test")).thenReturn(Optional.of(createTestFile()));
        when(pythonScriptInvoker.executeAsMap(anyString(), anyString(), anyMap()))
                .thenReturn(Map.of("success", true, "payload", Map.of()));
        for (Map<String, Object> r : List.of(
                service.getTurbulence("wrf_test"),
                service.getVisibility("wrf_test"),
                service.getLightningRisk("wrf_test"))) {
            assertEquals(Boolean.FALSE, r.get("success"), "应显式失败而不是回退常量: " + r);
            assertTrue(String.valueOf(r.get("message")).contains("不回退"),
                    "错误信息应写清「不回退到占位常量」: " + r.get("message"));
        }
    }

    @Test
    @DisplayName("获取高度分层数据")
    @SuppressWarnings("unchecked")
    void testGetHeightLayers() {
        when(repository.findByFileId("wrf_test")).thenReturn(Optional.of(createTestFile()));
        Map<String, Object> result = service.getHeightLayers("wrf_test", List.of(0, 100, 1000));
        assertTrue((Boolean) result.get("success"));
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(3, data.get("layer_count"));
    }

    @Test
    @DisplayName("文件不存在时返回错误")
    void testFileNotFound() {
        when(repository.findByFileId("not_exist")).thenReturn(Optional.empty());
        Map<String, Object> result = service.getWeatherData("not_exist");
        assertFalse((Boolean) result.get("success"));
    }

    @Test
    @DisplayName("获取详情")
    void testGetDetail() {
        when(repository.findById(1L)).thenReturn(Optional.of(createTestFile()));
        Map<String, Object> result = service.getDetail(1L);
        assertTrue((Boolean) result.get("success"));
    }
}
