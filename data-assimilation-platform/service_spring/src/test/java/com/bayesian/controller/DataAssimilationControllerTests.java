package com.bayesian.controller;

import com.bayesian.dto.request.AssimilationRequest;
import com.bayesian.service.AssimilationService;
import com.bayesian.service.CacheService;
import com.bayesian.service.VarianceFieldService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("同化平台控制器测试")
class DataAssimilationControllerTests {

    @Mock
    private AssimilationService assimilationService;

    @Mock
    private CacheService cacheService;

    @Mock
    private VarianceFieldService varianceFieldService;

    @InjectMocks
    private AssimilationController assimilationController;

    @InjectMocks
    private VarianceFieldController varianceFieldController;

    @InjectMocks
    private DataController dataController;

    @InjectMocks
    private ResilienceController resilienceController;

    @Test
    @DisplayName("执行同化")
    void testRunAssimilation() {
        AssimilationRequest request = new AssimilationRequest();
        request.setMethod("3dvar");
        request.setDomainSize("1000,1000,100");
        request.setResolution(50.0);

        when(assimilationService.run(any())).thenReturn(Map.of("status", "success"));
        var result = assimilationController.runAssimilation(request);
        assertNotNull(result);
    }

    @Test
    @DisplayName("获取同化状态")
    void testGetStatus() {
        when(assimilationService.getStatus()).thenReturn(Map.of("status", "idle"));
        var result = assimilationController.getStatus();
        assertNotNull(result);
    }

    @Test
    @DisplayName("获取方差场")
    void testGetVarianceField() {
        when(varianceFieldService.getVarianceField(anyString())).thenReturn(Map.of("variance", new double[]{1.0, 2.0}));
        var result = varianceFieldController.getVarianceField("test-job");
        assertNotNull(result);
    }

    @Test
    @DisplayName("获取数据列表")
    void testGetDataList() {
        var result = dataController.getDataList();
        assertTrue((Boolean) result.get("success"));
    }

    @Test
    @DisplayName("获取健康状态")
    void testGetHealth() {
        var result = dataController.getHealth();
        assertTrue((Boolean) result.get("success"));
    }

    @Test
    @DisplayName("获取韧性状态")
    void testGetResilienceStatus() {
        var result = resilienceController.getResilienceStatus();
        assertTrue((Boolean) result.get("success"));
    }
}