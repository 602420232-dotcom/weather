package com.uav.wrf.processor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ★ 2026-09-28 新增：C2 的「真实数值 → 业务结论」映射层单测。
 *
 * <p>为什么要有它：C2 的验收判据是「三项都从实际数据算出」。
 * {@code WrfPlatformIntegrationTest} 证明的是**端到端确实调到了解析器**；
 * 这里锁住的是**分档边界**——否则以后有人把阈值改松/改紧，端到端测试仍然会绿。
 *
 * <p>阈值全部引用 {@link WrfRiskMetrics} 的公开常量，不在测试里重复写数字。
 */
@DisplayName("WrfRiskMetrics 分档与打分")
class WrfRiskMetricsTest {

    @Test
    @DisplayName("TKE 分档边界")
    void tkeBands() {
        assertEquals("NEGLIGIBLE", WrfRiskMetrics.tkeIntensity(WrfRiskMetrics.TKE_LOW - 0.001));
        assertEquals("LOW", WrfRiskMetrics.tkeIntensity(WrfRiskMetrics.TKE_LOW));
        assertEquals("MODERATE", WrfRiskMetrics.tkeIntensity(WrfRiskMetrics.TKE_MODERATE));
        assertEquals("SEVERE", WrfRiskMetrics.tkeIntensity(WrfRiskMetrics.TKE_SEVERE));
    }

    @Test
    @DisplayName("EDR = ε^(1/3)，ε≤0 时为 0")
    void edrFromDissipation() {
        assertEquals(0.0, WrfRiskMetrics.edrFromDissipation(0.0), 1e-12);
        assertEquals(0.0, WrfRiskMetrics.edrFromDissipation(-1.0), 1e-12);
        assertEquals(0.3, WrfRiskMetrics.edrFromDissipation(0.027), 1e-9);
    }

    @Test
    @DisplayName("能见度分档用域最小；域平均只影响 *_mean 与头条值")
    void visibilityBands() {
        assertEquals("GOOD", WrfRiskMetrics.visibilityCategory(WrfRiskMetrics.VIS_VFR_M));
        assertEquals("MODERATE", WrfRiskMetrics.visibilityCategory(WrfRiskMetrics.VIS_MVFR_M));
        assertEquals("POOR", WrfRiskMetrics.visibilityCategory(WrfRiskMetrics.VIS_IFR_M));
        assertEquals("VERY_POOR", WrfRiskMetrics.visibilityCategory(WrfRiskMetrics.VIS_IFR_M - 1));
        // 均值 GOOD、最小 VERY_POOR 的「被平均掩盖」情形必须同时暴露出来
        Map<String, Object> r = WrfRiskMetrics.visibility(50.0, 12000.0, 0.4, 15.0, 80.0);
        assertEquals("VERY_POOR", r.get("visibility_category"));
        assertEquals("GOOD", r.get("visibility_category_mean"));
        assertEquals(Boolean.FALSE, r.get("flyable"));
        assertEquals("LIFR", r.get("flight_category"));
    }

    @Test
    @DisplayName("湍流端点：分档取最大值；min>max 时被夹住")
    void turbulenceEndpoint() {
        Map<String, Object> r = WrfRiskMetrics.turbulence(0.01, 2.0, 5.0, 0.027, 100);
        assertEquals("MODERATE", r.get("turbulence_intensity"));
        assertEquals(2.0, ((Number) r.get("tke_max")).doubleValue(), 1e-9);
        assertEquals(2.0, ((Number) r.get("tke_min")).doubleValue(), 1e-9, "min 不应大于 max");
        assertEquals(Boolean.FALSE, r.get("flyable"), "tke_max > TKE_FLYABLE_MAX ⇒ 不可飞");
    }

    @Test
    @DisplayName("闪电打分：CAPE 项饱和、CIN 抑制归零、LCL 线性")
    void lightningScore() {
        double zero = WrfRiskMetrics.lightningRiskScore(0.0, -WrfRiskMetrics.CIN_SUPPRESS_JKG,
                WrfRiskMetrics.LCL_LO_M);
        assertEquals(0.0, zero, 1e-9, "CAPE=0、CIN 完全抑制、LCL 取下界 ⇒ 0 分");

        double sat = WrfRiskMetrics.lightningRiskScore(WrfRiskMetrics.CAPE_SAT_JKG, 0.0,
                WrfRiskMetrics.LCL_HI_M);
        assertEquals(WrfRiskMetrics.W_CAPE + WrfRiskMetrics.W_CIN + WrfRiskMetrics.W_LCL, sat, 1e-9,
                "CAPE 饱和 + 无抑制 + LCL 取上界 ⇒ 满分");

        assertTrue(WrfRiskMetrics.lightningRiskScore(1000.0, -10.0, 1000.0)
                        < WrfRiskMetrics.lightningRiskScore(2000.0, -10.0, 1000.0),
                "同条件下 CAPE 越大分越高（打分确实随输入变，不是常量）");
        assertEquals("SEVERE", WrfRiskMetrics.riskLevel(WrfRiskMetrics.RISK_SEVERE));
        assertEquals("HIGH", WrfRiskMetrics.riskLevel(WrfRiskMetrics.RISK_HIGH));
        assertEquals("MODERATE", WrfRiskMetrics.riskLevel(WrfRiskMetrics.RISK_MODERATE));
        assertEquals("LOW", WrfRiskMetrics.riskLevel(0.0));
    }

    @Test
    @DisplayName("★ 端点输出的数值就是解析器给的数——不是 1.2 / 8500 / 28.5 这类占位常量")
    void notTheOldHardcodedConstants() {
        Map<String, Object> t = WrfRiskMetrics.turbulence(0.0001, 0.848, 0.0, 0.0, 100);
        assertNotEquals(1.2, ((Number) t.get("tke_mean")).doubleValue(), 1e-9);
        assertNotEquals(2.8, ((Number) t.get("tke_max")).doubleValue(), 1e-9);
        Map<String, Object> v = WrfRiskMetrics.visibility(50.0, 17562.9, 14.045, 15.41, 95.22);
        assertNotEquals(8500.0, ((Number) v.get("visibility_m")).doubleValue(), 1e-9);
        assertNotEquals(0.46, ((Number) v.get("extinction_coefficient")).doubleValue(), 1e-9);
        Map<String, Object> l = WrfRiskMetrics.lightningRisk(1716.4, 18.9, -1000.0, 65.7, 165.9);
        assertEquals(1716.4, ((Number) l.get("cape")).doubleValue(), 1e-6);
        assertNotEquals(680.0, ((Number) l.get("cape")).doubleValue(), 1e-9);
        assertNotEquals(28.5, ((Number) l.get("risk_score")).doubleValue(), 1e-9);
    }
}
