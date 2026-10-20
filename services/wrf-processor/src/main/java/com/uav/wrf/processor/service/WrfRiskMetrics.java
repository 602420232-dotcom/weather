package com.uav.wrf.processor.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WRF 风险诊断量的「真实数值 → 业务结论」映射层。
 *
 * <p>★ 本类是 <b>C2</b> 项的修复产物。修复前 {@code WrfDataService} 的三个端点
 * （{@code /turbulence}、{@code /visibility}、{@code /lightning-risk}）直接返回
 * 硬编码常量（{@code tke_mean=1.2}、{@code visibility_m=8500.0}、{@code risk_score=28.5}、
 * {@code cape=680.0} …）。现在由
 * 「{@code wrf_processor.py} 算出的真实场 → 本类分档/打分 → 端点输出」取代。
 *
 * <p>★ <b>三条设计纪律</b>（与 {@code docs/27 §7.6.9} 的论文口径一致）：
 * <ol>
 *   <li><b>本类不做任何物理计算</b>，只做分档与打分。物理量全部来自
 *       {@code wrf_processor.py}（该解析器已逐项验证：物理内核 19 项 + 真实 wrfout 端到端 45 项）。</li>
 *   <li>返回值里必须带 {@code method} / {@code caveat} 字段，写清它是
 *       <b>参数化估计</b>还是<b>模式诊断量</b>。本项目真实 wrfout
 *       （{@code bl_pbl_physics=1}, YSU）<b>没有</b> {@code QKE/TKE/CAPE/CIN/LCL}
 *       ⇒ 湍流与对流量<b>一定是参数化</b>，论文里不能写成「WRF 诊断输出」。</li>
 *   <li>分档阈值是<b>工程口径</b>，<b>不是本项目实测标定</b>的阈值。全部集中在类顶部，
 *       改一处即全局生效；回应里同时回显阈值，使「输入 → 输出」可复算。</li>
 * </ol>
 *
 * <p>★ 本类刻意保持为<b>纯函数 + 无依赖</b>（不 import Spring / Jackson / IO），
 * 这样才能被一个最小 harness 直接调用、把「输入 → 输出」逐条复现出来。
 *
 * @since 2026-09-26（平台侧 C2）
 */
public final class WrfRiskMetrics {

    private WrfRiskMetrics() {
    }

    // ══════════════════════════════════════════════════════════════════════
    //  阈值常量的唯一来源（可复算）
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 湍流分档（TKE，单位 m²/s²）。
     *
     * <p>沿用平台既有约定（{@code WrfDataService.getHeightLayers} 原本就用
     * 0.5 / 1.5 / 3.0 三档），以便下游无需改动即可继续工作。
     *
     * <p>⚠️ 这是<b>工程分档</b>，不是本项目实测标定值，也不是 ICAO 标准。
     */
    public static final double TKE_LOW = 0.5;

    /** 见 {@link #TKE_LOW}。 */
    public static final double TKE_MODERATE = 1.5;

    /** 见 {@link #TKE_LOW}。 */
    public static final double TKE_SEVERE = 3.0;

    /**
     * 湍流分档（EDR = ε^(1/3)，单位 m^(2/3)·s⁻¹）。
     *
     * <p>EDR（涡流耗散率）是航空界通用的湍流强度度量，与机型无关。
     * 下面三档取业界常见的量级分界：轻度 ≈ 0.15、中度 ≈ 0.30、重度 ≈ 0.50。
     *
     * <p>⚠️ <b>必须标注</b>：本项目的 ε 来自 k-l 一阶闭合平衡估计
     * （{@code wrf_processor.py} 的 {@code dissipation_rate}），
     * <b>不是飞机实测 EDR、也不是模式输出</b>。所以本类只把它当
     * 「比 TKE 更贴近航空口径的参考量」，不参与任何安全性判定。
     */
    public static final double EDR_LIGHT = 0.15;

    /** 见 {@link #EDR_LIGHT}。 */
    public static final double EDR_MODERATE = 0.30;

    /** 见 {@link #EDR_LIGHT}。 */
    public static final double EDR_SEVERE = 0.50;

    /**
     * 能见度分档（米），US NOAA/NWS 飞行规则分类的米制换算：
     * VFR &gt; 5 statute mile (8047 m) / MVFR 3–5 sm (4828–8047 m) /
     * IFR 1–3 sm (1609–4828 m) / LIFR &lt; 1 sm (1609 m)。
     *
     * <p>⚠️ 该分类是为<b>有人机</b>目视飞行规则定义的；无人机没有座舱目视，
     * 这里只借它的量级排序，<b>不等于</b>无人机的运行标准。
     */
    public static final double VIS_VFR_M = 8000.0;

    /** 见 {@link #VIS_VFR_M}。 */
    public static final double VIS_MVFR_M = 4800.0;

    /** 见 {@link #VIS_VFR_M}。 */
    public static final double VIS_IFR_M = 1600.0;

    /**
     * 闪电风险打分权重（合计 100）。
     *
     * <p>{@code risk_score = CAPE 项(0~70) + CIN 项(0~20) + LCL 项(0~10)}。
     */
    public static final double W_CAPE = 70.0;

    /** 见 {@link #W_CAPE}。 */
    public static final double W_CIN = 20.0;

    /** 见 {@link #W_CAPE}。 */
    public static final double W_LCL = 10.0;

    /**
     * CAPE 项的饱和值（J/kg）。取 2500 是「强对流」的常见量级；
     * 超过它之后打分不再增加（避免单个极值把分数拉爆）。
     */
    public static final double CAPE_SAT_JKG = 2500.0;

    /** CIN 的完全抑制阈值（J/kg，取绝对值）。|CIN| ≥ 该值时 CIN 项得 0 分。 */
    public static final double CIN_SUPPRESS_JKG = 200.0;

    /** LCL 项的下界 / 上界（m AGL）。 */
    public static final double LCL_LO_M = 500.0;

    /** 见 {@link #LCL_LO_M}。 */
    public static final double LCL_HI_M = 3000.0;

    /** {@code risk_score} 的分档边界。 */
    public static final double RISK_MODERATE = 25.0;

    /** 见 {@link #RISK_MODERATE}。 */
    public static final double RISK_HIGH = 50.0;

    /** 见 {@link #RISK_MODERATE}。 */
    public static final double RISK_SEVERE = 75.0;

    /** 湍流「可飞」判据：最大 TKE 低于此值且 EDR 未达中度。⚠️ 工程建议值。 */
    public static final double TKE_FLYABLE_MAX = 1.5;

    /** 能见度「可飞」判据（米）。⚠️ 工程建议值，非法规。 */
    public static final double VIS_FLYABLE_MIN_M = 5000.0;

    /**
     * ★★ 2026-10-01 新增：`flyable` 的**并列字段**，名字自带"这是工程提示"。
     *
     * <p><b>为什么加</b>：三个端点都会算出一个 {@code flyable} 布尔，
     * 而它由**未标定的工程阈值**决定（见类顶注释③）。
     * 「{@code flyable: true}」这种字段名极易被下游当作**放飞放行依据** ——
     * 而本项目**没有任何一条阈值经过实测标定**（唯一有真值的是风速 8 m/s 安全红线，
     * 那在 `x22_wind_scores.py`，不在本类）。
     *
     * <p>处置取「**改名 + 保留**」而不是「直接改名」：
     * <ul>
     *   <li>新增 {@code flyable_engineering_hint} —— 新代码应读它，语义无歧义；</li>
     *   <li>保留 {@code flyable} —— **不破坏既有读取方**（与
     *       {@code lightningRisk} 里"保留 {@code cape/cin/lcl} 旧字段名"同一取向）；</li>
     *   <li>两个字段**同值**，且响应里加 {@code flyable_is_engineering_only=true}
     *       与一句明确的禁用说明 ⇒ 读的人不可能不知道它的性质。</li>
     * </ul>
     */
    private static final String FLYABLE_NOTE =
            "该字段由**未标定的工程阈值**得出，仅供界面提示，"
            + "**不得**作为放飞/放行决策依据；本项目唯一经实测标定的阈值是 "
            + "x22_wind_scores.py 里的风速安全红线 8 m/s";

    /** 把三个端点共用的"flyable 只能当工程提示"这件事一次写清。 */
    private static void putFlyable(Map<String, Object> m, boolean v) {
        m.put("flyable", v);
        m.put("flyable_engineering_hint", v);
        m.put("flyable_is_engineering_only", true);
        m.put("flyable_note", FLYABLE_NOTE);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  1. 湍流  →  /turbulence
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TKE → 湍流强度标签（沿用平台既有分档）。
     *
     * @param tke 湍流动能，m²/s²
     */
    public static String tkeIntensity(double tke) {
        if (tke >= TKE_SEVERE) {
            return "SEVERE";
        }
        if (tke >= TKE_MODERATE) {
            return "MODERATE";
        }
        if (tke >= TKE_LOW) {
            return "LOW";
        }
        return "NEGLIGIBLE";
    }

    /** EDR (m^(2/3)·s⁻¹) → 航空湍流强度标签。 */
    public static String edrIntensity(double edr) {
        if (edr >= EDR_SEVERE) {
            return "SEVERE";
        }
        if (edr >= EDR_MODERATE) {
            return "MODERATE";
        }
        if (edr >= EDR_LIGHT) {
            return "LIGHT";
        }
        return "NEGLIGIBLE";
    }

    /** ε (m²/s³) → EDR = ε^(1/3)。ε ≤ 0 时返回 0。 */
    public static double edrFromDissipation(double dissipationRate) {
        if (!(dissipationRate > 0)) {
            return 0.0;
        }
        return Math.cbrt(dissipationRate);
    }

    private static String turbulenceAdvice(String intensity) {
        switch (intensity) {
            case "SEVERE":
                return "重度湍流 —— 不建议起飞，或在显著更高/更低高度层重新规划航线";
            case "MODERATE":
                return "中度湍流 —— 需谨慎飞行，建议避让并降低巡航速度";
            case "LOW":
                return "轻度湍流 —— 可飞，注意姿态扰动";
            default:
                return "湍流可忽略 —— 可正常飞行";
        }
    }

    /**
     * 湍流端点响应体。
     *
     * <p>分档取<b>最大值</b>（{@code tke_max}）：运行上真正构成危害的是
     * 「域内最坏的那一层」，与 {@code getHeightLayers} 的逐层构造方式一致。
     * 同时给出按平均值判定的档位 {@code turbulence_intensity_mean}，避免只给一个口径。
     *
     * @param tkeMean         域平均 TKE（m²/s²）
     * @param tkeMax          域最大 TKE（m²/s²）
     * @param tkeMin          域最小 TKE（m²/s²）
     * @param dissipationRate 域平均耗散率 ε（m²/s³）
     * @param height          该记录配置的参考高度（m AGL），原样回显
     */
    public static Map<String, Object> turbulence(Double tkeMean, Double tkeMax, Double tkeMin,
                                                 Double dissipationRate, Integer height) {
        double mean = nz(tkeMean);
        double max = nz(tkeMax);
        double min = nz(tkeMin);
        double eps = nz(dissipationRate);

        // ⚠️ 解析器在极端情况下可能给 0/负值；这里把 min 夹到不超过 max，
        //    避免出现 "tke_min > tke_max" 这种会被下游当成数据错误的结果。
        if (min > max) {
            min = max;
        }
        double edr = edrFromDissipation(eps);
        String intensity = tkeIntensity(max);
        String edrBand = edrIntensity(edr);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tke_mean", round(mean, 4));
        m.put("tke_max", round(max, 4));
        m.put("tke_min", round(min, 4));
        m.put("tke_unit", "m2 s-2");
        m.put("dissipation_rate", round(eps, 4));
        m.put("edr", round(edr, 4));
        m.put("edr_unit", "m^(2/3) s-1");
        m.put("edr_intensity", edrBand);

        m.put("turbulence_intensity", intensity);
        m.put("turbulence_intensity_mean", tkeIntensity(mean));
        m.put("turbulence_description", turbulenceAdvice(intensity));

        m.put("height", height);
        putFlyable(m, max < TKE_FLYABLE_MAX && !"MODERATE".equals(edrBand)
                && !"SEVERE".equals(edrBand));

        // ── 可复算所需的一切 ──
        Map<String, Object> bands = new LinkedHashMap<>();
        bands.put("tke_low", TKE_LOW);
        bands.put("tke_moderate", TKE_MODERATE);
        bands.put("tke_severe", TKE_SEVERE);
        bands.put("edr_light", EDR_LIGHT);
        bands.put("edr_moderate", EDR_MODERATE);
        bands.put("edr_severe", EDR_SEVERE);
        bands.put("flyable_tke_max", TKE_FLYABLE_MAX);
        m.put("tke_bands", bands);

        m.put("method", "k-l equilibrium TKE from wrf_processor.py "
                + "(NOT a model diagnostic: no QKE/TKE in this wrfout)");
        m.put("caveat", "分档阈值为工程口径（沿用平台既有 0.5/1.5/3.0），"
                + "EDR 由 ε^(1/3) 换算、ε 亦为参数化估计 ⇒ 不得当作适航判据");
        m.put("timestamp", System.currentTimeMillis());
        return m;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  2. 能见度  →  /visibility
    // ══════════════════════════════════════════════════════════════════════

    /** 能见度（米）→ 平台既有标签 GOOD / MODERATE / POOR / VERY_POOR。 */
    public static String visibilityCategory(double visM) {
        if (visM >= VIS_VFR_M) {
            return "GOOD";
        }
        if (visM >= VIS_MVFR_M) {
            return "MODERATE";
        }
        if (visM >= VIS_IFR_M) {
            return "POOR";
        }
        return "VERY_POOR";
    }

    /** 能见度（米）→ US NOAA/NWS 飞行规则分类 VFR / MVFR / IFR / LIFR。 */
    public static String flightCategory(double visM) {
        if (visM >= VIS_VFR_M) {
            return "VFR";
        }
        if (visM >= VIS_MVFR_M) {
            return "MVFR";
        }
        if (visM >= VIS_IFR_M) {
            return "IFR";
        }
        return "LIFR";
    }

    private static String visibilityAdvice(String category) {
        switch (category) {
            case "GOOD":
                return "能见度良好，可正常飞行";
            case "MODERATE":
                return "能见度中等 —— 可飞，但需保持视线内飞行并留足返航余量";
            case "POOR":
                return "能见度差 —— 不建议目视飞行，须依赖超视距手段";
            default:
                return "能见度极差 —— 不建议起飞";
        }
    }

    /**
     * 能见度端点响应体。
     *
     * <p>★ 口径：{@code visibility_m} 给<b>域平均</b>（表述「这片空域的能见度」），
     * 分档用<b>域最小</b>（运行上最坏点决定能不能飞）。两个口径都回显，
     * 避免出现「均值 GOOD 但某处 200 m」这种被平均掩盖的危险。
     *
     * @param visMinM          域最小能见度（m）
     * @param visMeanM         域平均能见度（m）
     * @param extinctionCoef   域平均消光系数（km⁻¹）
     * @param temperatureC     2 m 温度（°C），可为 null
     * @param humidityPercent  2 m 相对湿度（%），可为 null
     */
    public static Map<String, Object> visibility(Double visMinM, Double visMeanM,
                                                 Double extinctionCoef,
                                                 Double temperatureC, Double humidityPercent) {
        double vMin = nz(visMinM);
        double vMean = nz(visMeanM);
        if (vMean < vMin) {
            vMean = vMin;
        }
        String category = visibilityCategory(vMin);
        String categoryMean = visibilityCategory(vMean);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visibility_m", round(vMean, 1));
        m.put("visibility_min_m", round(vMin, 1));
        m.put("visibility_max_source", "not returned by parser (domain mean used as headline)");
        m.put("extinction_coefficient", round(nz(extinctionCoef), 4));
        m.put("extinction_unit", "km-1");
        m.put("visibility_category", category);
        m.put("visibility_category_mean", categoryMean);
        m.put("flight_category", flightCategory(vMin));
        m.put("visibility_description", visibilityAdvice(category));

        if (temperatureC != null) {
            m.put("temperature_c", round(temperatureC, 2));
        }
        if (humidityPercent != null) {
            m.put("humidity_percent", round(humidityPercent, 2));
        }
        putFlyable(m, vMin >= VIS_FLYABLE_MIN_M);

        Map<String, Object> bands = new LinkedHashMap<>();
        bands.put("vfr_min_m", VIS_VFR_M);
        bands.put("mvfr_min_m", VIS_MVFR_M);
        bands.put("ifr_min_m", VIS_IFR_M);
        bands.put("flyable_min_m", VIS_FLYABLE_MIN_M);
        m.put("visibility_bands", bands);

        m.put("method", "Stoelinga & Warner (1999) RH-visibility relation, "
                + "computed in wrf_processor.py");
        m.put("caveat", "该能见度为 RH 经验参数化，非第一性原理、亦非观测；"
                + "分档借用 FAA 飞行规则量级（为有人机目视飞行定义）");
        m.put("timestamp", System.currentTimeMillis());
        return m;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  3. 闪电风险  →  /lightning-risk
    // ══════════════════════════════════════════════════════════════════════

    /**
     * CAPE / CIN / LCL → 闪电风险打分（0~100）。
     *
     * <p>三项线性加权：CAPE 项（0~70，饱和于 {@link #CAPE_SAT_JKG}）、
     * CIN 项（0~20，|CIN| ≥ {@link #CIN_SUPPRESS_JKG} 归零）、
     * LCL 项（0~10，{@link #LCL_LO_M}~{@link #LCL_HI_M} 线性）。
     *
     * <p>物理含义：CAPE 是对流有效位能（潜势），CIN 是抑制层（现实闸门），
     * LCL 越高表示近地面越干、云底越高。三者都是<b>探空尺度的潜势指标</b>，
     * <b>不是闪电探测</b>。
     */
    public static double lightningRiskScore(double capeMax, double cinMin, double lclMean) {
        double capeTerm = W_CAPE * clamp01(capeMax / CAPE_SAT_JKG);
        double cinTerm = W_CIN * clamp01(1.0 - Math.abs(cinMin) / CIN_SUPPRESS_JKG);
        double lclTerm = W_LCL * clamp01((lclMean - LCL_LO_M) / (LCL_HI_M - LCL_LO_M));
        return capeTerm + cinTerm + lclTerm;
    }

    /** 风险分数 → LOW / MODERATE / HIGH / SEVERE。 */
    public static String riskLevel(double score) {
        if (score >= RISK_SEVERE) {
            return "SEVERE";
        }
        if (score >= RISK_HIGH) {
            return "HIGH";
        }
        if (score >= RISK_MODERATE) {
            return "MODERATE";
        }
        return "LOW";
    }

    private static String riskAdvice(String level) {
        switch (level) {
            case "SEVERE":
                return "闪电风险很高 —— 禁止起飞，任务应改期";
            case "HIGH":
                return "闪电风险高 —— 不建议起飞，需重新规划时刻或航线";
            case "MODERATE":
                return "闪电风险中等 —— 可飞但需持续监视对流发展";
            default:
                return "闪电风险低 —— 可正常飞行";
        }
    }

    /**
     * 闪电风险端点响应体。
     *
     * <p>★ {@code cape} / {@code cin} / {@code lcl} 三个字段名保持与原硬编码版本一致
     * （原值 680.0 / −35.0 / 850.0），这样既有的字段读取方不需要改；现在它们的值是
     * <b>真实 wrfout 算出来的</b>。同时新增 {@code cape_mean_jkg} 等更明确的名字。
     *
     * @param capeMax  域最大 CAPE（J/kg）
     * @param capeMean 域平均 CAPE（J/kg）
     * @param cinMin   域最小 CIN（J/kg，≤ 0）
     * @param lclMean  域平均抬升凝结高度（m AGL）
     * @param lfcMean  域平均自由对流高度（m AGL）
     */
    public static Map<String, Object> lightningRisk(Double capeMax, Double capeMean, Double cinMin,
                                                    Double lclMean, Double lfcMean) {
        double cMax = nz(capeMax);
        double cMean = nz(capeMean);
        double ci = nz(cinMin);
        double lcl = nz(lclMean);
        double lfc = nz(lfcMean);

        double score = lightningRiskScore(cMax, ci, lcl);
        String level = riskLevel(score);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cape", round(cMax, 1));
        m.put("cin", round(ci, 1));
        m.put("lcl", round(lcl, 1));
        m.put("cape_mean_jkg", round(cMean, 1));
        m.put("lfc_mean_m", round(lfc, 1));
        m.put("risk_score", round(score, 1));
        m.put("risk_level", level);
        m.put("risk_description", riskAdvice(level));
        m.put("thunderstorm_potential", score >= RISK_HIGH);
        putFlyable(m, score < RISK_MODERATE);

        Map<String, Object> terms = new LinkedHashMap<>();
        terms.put("cape_term", round(W_CAPE * clamp01(cMax / CAPE_SAT_JKG), 2));
        terms.put("cin_term", round(W_CIN * clamp01(1.0 - Math.abs(ci) / CIN_SUPPRESS_JKG), 2));
        terms.put("lcl_term", round(W_LCL * clamp01((lcl - LCL_LO_M) / (LCL_HI_M - LCL_LO_M)), 2));
        m.put("score_terms", terms);

        Map<String, Object> bands = new LinkedHashMap<>();
        bands.put("cape_sat_jkg", CAPE_SAT_JKG);
        bands.put("cin_suppress_jkg", CIN_SUPPRESS_JKG);
        bands.put("lcl_lo_m", LCL_LO_M);
        bands.put("lcl_hi_m", LCL_HI_M);
        bands.put("score_moderate", RISK_MODERATE);
        bands.put("score_high", RISK_HIGH);
        bands.put("score_severe", RISK_SEVERE);
        m.put("risk_bands", bands);

        m.put("method", "parcel ascent from T2/Q2/PSFC in wrf_processor.py "
                + "(NOT a model diagnostic: no CAPE/CIN in this wrfout); "
                + "score = 70*CAPE + 20*CIN + 10*LCL weights");
        m.put("caveat", "这是基于探空尺度潜势指标的参数化指数，"
                + "不是闪电探测/预报；阈值与权重为工程取值，本项目未做实测标定");
        m.put("timestamp", System.currentTimeMillis());

        List<String> notes = new ArrayList<>();
        notes.add("CAPE 与 CIN 为气块抬升法计算，非模式诊断量");
        if (ci > -10.0 && cMax > 1000.0) {
            notes.add("CAPE > 1000 J/kg 且 |CIN| < 10 J/kg ⇒ 对流一旦触发即可能强盛");
        }
        m.put("notes", notes);
        return m;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  小工具
    // ══════════════════════════════════════════════════════════════════════

    private static double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }

    /** 四舍五入到 nd 位小数；NaN/Inf 原样返回（不抛异常）。 */
    public static double round(double v, int nd) {
        if (!Double.isFinite(v)) {
            return v;
        }
        double f = Math.pow(10, nd);
        return Math.round(v * f) / f;
    }
}
