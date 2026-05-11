package com.uav.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PythonAlgorithmUtil安全测试")
class PythonAlgorithmUtilTest {

    private PythonAlgorithmUtil pythonAlgorithmUtil;

    @BeforeEach
    void setUp() {
        pythonAlgorithmUtil = new PythonAlgorithmUtil();
        ReflectionTestUtils.setField(pythonAlgorithmUtil, "scriptPath", "./python");
        ReflectionTestUtils.setField(pythonAlgorithmUtil, "timeout", 30000);
    }

    @Test
    @DisplayName("测试执行白名单脚本 - wrf_parser.py")
    void testExecuteValidScript_WrfParser() {
        Map<String, Object> params = new HashMap<>();
        params.put("input", "test_data");
        assertDoesNotThrow(() -> {
            String result = pythonAlgorithmUtil.executePythonScript("wrf/wrf_parser.py", params);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("测试执行白名单脚本 - bayesian_assimilation.py")
    void testExecuteValidScript_BayesianAssimilation() {
        Map<String, Object> params = new HashMap<>();
        params.put("observations", new double[]{1.0, 2.0, 3.0});
        assertDoesNotThrow(() -> {
            String result = pythonAlgorithmUtil.executePythonScript("assimilation/bayesian_assimilation.py", params);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("测试执行白名单脚本 - meteor_forecast.py")
    void testExecuteValidScript_MeteorForecast() {
        Map<String, Object> params = new HashMap<>();
        params.put("location", "Beijing");
        params.put("hours", 24);
        assertDoesNotThrow(() -> {
            String result = pythonAlgorithmUtil.executePythonScript("prediction/meteor_forecast.py", params);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("测试执行白名单脚本 - three_layer_planner.py")
    void testExecuteValidScript_ThreeLayerPlanner() {
        Map<String, Object> params = new HashMap<>();
        params.put("start", new double[]{0, 0, 0});
        params.put("end", new double[]{100, 100, 50});
        assertDoesNotThrow(() -> {
            String result = pythonAlgorithmUtil.executePythonScript("path-planning/three_layer_planner.py", params);
            assertNotNull(result);
        });
    }

    @Test
    @DisplayName("测试拒绝非法脚本名称")
    void testRejectInvalidScriptName() {
        String result = pythonAlgorithmUtil.executePythonScript("malicious.py", new HashMap<>());
        assertTrue(result.contains("安全验证失败"));
    }

    @Test
    @DisplayName("测试拒绝路径遍历攻击 - ../etc/passwd")
    void testRejectPathTraversal() {
        String result = pythonAlgorithmUtil.executePythonScript("../etc/passwd", new HashMap<>());
        assertTrue(result.contains("安全验证失败") || result.contains("非法字符") || result.contains("未授权的脚本"));
    }

    @Test
    @DisplayName("测试拒绝空脚本名称")
    void testRejectEmptyScriptName() {
        String result = pythonAlgorithmUtil.executePythonScript("", new HashMap<>());
        assertTrue(result.contains("安全验证失败"));
    }

    @Test
    @DisplayName("测试拒绝空白脚本名称")
    void testRejectBlankScriptName() {
        String result = pythonAlgorithmUtil.executePythonScript("   ", new HashMap<>());
        assertTrue(result.contains("安全验证失败"));
    }

    @Test
    @DisplayName("测试拒绝包含~的脚本名称")
    void testRejectScriptWithTilde() {
        String result = pythonAlgorithmUtil.executePythonScript("~/malicious.py", new HashMap<>());
        assertTrue(result.contains("安全验证失败"));
    }

    @Test
    @DisplayName("测试脚本名称验证 - 包含双点")
    void testRejectScriptWithDoubleDots() {
        String result = pythonAlgorithmUtil.executePythonScript("../malicious.py", new HashMap<>());
        assertTrue(result.contains("安全验证失败"));
    }

    @Test
    @DisplayName("测试null脚本名称")
    void testRejectNullScriptName() {
        String result = pythonAlgorithmUtil.executePythonScript(null, new HashMap<>());
        assertTrue(result.contains("安全验证失败"));
    }
}
