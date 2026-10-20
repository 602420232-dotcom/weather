package com.uav.wrf.processor.service;

import com.uav.common.script.PythonScriptInvoker;
import com.uav.wrf.processor.entity.WrfDataFile;
import com.uav.wrf.processor.repository.WrfDataFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WrfDataService {

    private final WrfDataFileRepository wrfDataFileRepository;

    /**
     * ★ C2 新增：三个诊断端点改为**从真实 wrfout 计算**，
     * 因此需要按 {@code WrfController} 的同一条链路调 Python 解析器。
     * 注意这里<b>不是</b>「新增依赖」，而是把 {@code WrfController.parseWrfFile}
     * 已经在用的同一条链路接到诊断端点上。
     */
    private final PythonScriptInvoker pythonScriptInvoker;

    /** 与 {@code WrfController} 同名的配置项，默认值与 {@code application.yml} 一致。 */
    @Value("${wrf.python-script:wrf_processor.py}")
    private String pythonScriptPath;

    public Optional<WrfDataFile> findByFileId(String fileId) {
        return wrfDataFileRepository.findByFileId(fileId);
    }

    public Optional<WrfDataFile> findById(Long id) {
        return wrfDataFileRepository.findById(Objects.requireNonNull(id));
    }

    public Page<WrfDataFile> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wrfDataFileRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public WrfDataFile createWrfDataFile(String fileName, String filePath, Long fileSize) {
        String fileId = "wrf_" + UUID.randomUUID().toString().substring(0, 8);
        WrfDataFile wrfDataFile = WrfDataFile.builder()
                .fileId(fileId)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .status("UPLOADED")
                .height(100)
                .timeSteps(24)
                .variables("temperature,humidity,wind_speed,wind_direction,pressure,turbulence,visibility,lightning_risk")
                .build();
        return wrfDataFileRepository.save(Objects.requireNonNull(wrfDataFile));
    }

    public WrfDataFile updateWrfDataFile(WrfDataFile wrfDataFile) {
        return wrfDataFileRepository.save(Objects.requireNonNull(wrfDataFile));
    }

    public Map<String, Object> getWeatherData(String fileId) {
        Optional<WrfDataFile> optionalWrfDataFile = findByFileId(fileId);
        if (optionalWrfDataFile.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        WrfDataFile wrfDataFile = optionalWrfDataFile.get();
        Map<String, Object> weatherData = new HashMap<>();
        weatherData.put("fileId", fileId);
        weatherData.put("height", wrfDataFile.getHeight());
        weatherData.put("timeSteps", wrfDataFile.getTimeSteps());
        weatherData.put("variables", wrfDataFile.getVariables() != null
            ? Arrays.asList(wrfDataFile.getVariables().split(","))
            : List.of());
        weatherData.put("wind_speed", generateGridData(10, 10, 5.0, 15.0));
        weatherData.put("wind_direction", generateGridData(10, 10, 0.0, 360.0));
        weatherData.put("temperature", generateGridData(10, 10, 15.0, 30.0));
        weatherData.put("humidity", generateGridData(10, 10, 40.0, 80.0));
        weatherData.put("pressure", generateGridData(10, 10, 1000.0, 1020.0));
        return Map.of("success", true, "data", weatherData);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ★ C2：三个诊断端点改为「真实 wrfout → wrf_processor.py → 分档」
    //
    //  修复前这三个方法直接 put 硬编码常量（tke_mean=1.2 / visibility_m=8500.0 /
    //  risk_score=28.5 / cape=680.0 …）。硬编码比「接口已定义、算法待接入」更糟：
    //  它看起来是通的（README 第 68 条已把这个陷阱记档）。
    //
    //  ★ 一条刻意的纪律：**解析失败时不回退到常量**，而是 success=false + 原因。
    //    理由与上面同一句话 —— 静默回退会让"看起来是通的"重新出现，
    //    而这次连日志都不容易被注意到。
    // ══════════════════════════════════════════════════════════════════════

    public Map<String, Object> getTurbulence(String fileId) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        Optional<WrfRisk> risk = loadRisk(optional.get());
        if (risk.isEmpty()) {
            return parseFailure(fileId, "湍流");
        }
        WrfRisk r = risk.get();
        Map<String, Object> turbulence = WrfRiskMetrics.turbulence(
                r.tkeMean, r.tkeMax, r.tkeMin, r.dissipationRate, optional.get().getHeight());
        turbulence.put("fileId", fileId);
        turbulence.put("dataSource", r.dataSource);
        return Map.of("success", true, "data", turbulence);
    }

    public Map<String, Object> getVisibility(String fileId) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        Optional<WrfRisk> risk = loadRisk(optional.get());
        if (risk.isEmpty()) {
            return parseFailure(fileId, "能见度");
        }
        WrfRisk r = risk.get();
        Map<String, Object> visibility = WrfRiskMetrics.visibility(
                r.visibilityMinM, r.visibilityMeanM, r.extinctionCoefficient,
                r.surfaceTemperatureC, r.surfaceHumidityPercent);
        visibility.put("fileId", fileId);
        visibility.put("dataSource", r.dataSource);
        return Map.of("success", true, "data", visibility);
    }

    public Map<String, Object> getLightningRisk(String fileId) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        Optional<WrfRisk> risk = loadRisk(optional.get());
        if (risk.isEmpty()) {
            return parseFailure(fileId, "闪电风险");
        }
        WrfRisk r = risk.get();
        Map<String, Object> lightning = WrfRiskMetrics.lightningRisk(
                r.capeMax, r.capeMean, r.cinMin, r.lclMean, r.lfcMean);
        lightning.put("fileId", fileId);
        lightning.put("dataSource", r.dataSource);
        return Map.of("success", true, "data", lightning);
    }

    /**
     * ★ C3：对<b>服务端已经持有</b>的文件按 fileId 解析。
     *
     * <p>存在的意义：分片上传合并之后，文件已经在服务器磁盘上了，
     * 前端不该再整文件 POST 一次（那是 2 倍流量）。而允许客户端直接传
     * {@code filePath}（{@code /parse/advanced} 就是那样）等于把任意文件读取
     * 的口子开到 API 上 —— 所以这里只接受 {@code fileId}，路径由服务端查库得到。
     *
     * <p>返回结构与 {@code POST /api/wrf/parse} 保持一致（{@code data} 仍是
     * <b>Python stdout 的原始字符串</b>），前端可以复用同一段解析代码。
     */
    public Map<String, Object> parseStoredFile(String fileId, Integer heightOverride) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "error", "File not found: " + fileId);
        }
        WrfDataFile file = optional.get();
        if (file.getFilePath() == null || file.getFilePath().isBlank()) {
            return Map.of("success", false, "error", "该记录没有 filePath");
        }
        String script = pythonScriptPath;
        if (script == null || script.isBlank()) {
            return Map.of("success", false, "error", "wrf.python-script 未配置");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("filePath", file.getFilePath());
        params.put("height", heightOverride != null
                ? heightOverride
                : (file.getHeight() != null ? file.getHeight() : 100));
        try {
            String result = pythonScriptInvoker.execute(script, "parse", params);
            return Map.of("success", true, "data", result, "fileId", fileId);
        } catch (Exception e) {
            log.error("按 fileId 解析失败 fileId={}", fileId, e);
            return Map.of("success", false, "error", "解析失败: " + e.getMessage());
        }
    }

    private Map<String, Object> parseFailure(String fileId, String what) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("fileId", fileId);
        m.put("message", "无法从真实 wrfout 计算" + what + " —— 解析器未返回 risk 块，"
                + "详见服务端日志（不回退到占位常量，以免掩盖链路故障）");
        return m;
    }

    /**
     * 调 Python 解析器并把自己需要的标量抽出来。
     *
     * <p>链路与 {@code WrfController.parseWrfFile} 完全一致：
     * {@code PythonScriptInvoker.execute(script, "parse", {filePath, height})}
     * → 子进程 {@code wrf_processor.py} → stdout JSON。
     *
     * <p>用 {@link PythonScriptInvoker#executeAsMap} 而不是 {@code execute}：
     * 后者返回的是 <b>stdout 原始字符串</b>，调用方还得自己反序列化一次。
     */
    private Optional<WrfRisk> loadRisk(WrfDataFile file) {
        String script = pythonScriptPath;
        if (script == null || script.isBlank()) {
            log.warn("wrf.python-script 未配置，无法计算真实风险量");
            return Optional.empty();
        }
        String filePath = file.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            log.warn("fileId={} 没有 filePath，无法计算真实风险量", file.getFileId());
            return Optional.empty();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("filePath", filePath);
        params.put("height", file.getHeight() != null ? file.getHeight() : 100);

        Map<String, Object> result;
        try {
            result = pythonScriptInvoker.executeAsMap(script, "parse", params);
        } catch (Exception e) {
            log.error("解析 wrfout 失败 fileId={} path={}", file.getFileId(), filePath, e);
            return Optional.empty();
        }
        if (result == null || !Boolean.TRUE.equals(result.get("success"))) {
            log.warn("解析器未成功 fileId={} result={}", file.getFileId(), result);
            return Optional.empty();
        }
        Object riskObj = result.get("risk");
        if (!(riskObj instanceof Map)) {
            log.warn("解析器输出里没有 risk 块 fileId={}（旧版脚本？）", file.getFileId());
            return Optional.empty();
        }
        try {
            return Optional.of(WrfRisk.from(result));
        } catch (RuntimeException e) {
            log.warn("risk 块字段缺失或类型异常 fileId={}: {}", file.getFileId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 解析器输出中本服务需要的标量的<b>唯一</b>落点。
     *
     * <p>刻意做成不可变值对象而不是到处 {@code ((Map)...).get(...)}：
     * 字段名改了会在<b>一处</b>编译期/构造期失败，而不是在三个端点里各自静默变 null。
     */
    static final class WrfRisk {
        final Double tkeMean;
        final Double tkeMax;
        final Double tkeMin;
        final Double dissipationRate;
        final Double visibilityMinM;
        final Double visibilityMeanM;
        final Double extinctionCoefficient;
        final Double capeMax;
        final Double capeMean;
        final Double cinMin;
        final Double lclMean;
        final Double lfcMean;
        final Double surfaceTemperatureC;
        final Double surfaceHumidityPercent;
        final String dataSource;

        private WrfRisk(Double tkeMean, Double tkeMax, Double tkeMin, Double dissipationRate,
                        Double visibilityMinM, Double visibilityMeanM, Double extinctionCoefficient,
                        Double capeMax, Double capeMean, Double cinMin, Double lclMean, Double lfcMean,
                        Double surfaceTemperatureC, Double surfaceHumidityPercent, String dataSource) {
            this.tkeMean = tkeMean;
            this.tkeMax = tkeMax;
            this.tkeMin = tkeMin;
            this.dissipationRate = dissipationRate;
            this.visibilityMinM = visibilityMinM;
            this.visibilityMeanM = visibilityMeanM;
            this.extinctionCoefficient = extinctionCoefficient;
            this.capeMax = capeMax;
            this.capeMean = capeMean;
            this.cinMin = cinMin;
            this.lclMean = lclMean;
            this.lfcMean = lfcMean;
            this.surfaceTemperatureC = surfaceTemperatureC;
            this.surfaceHumidityPercent = surfaceHumidityPercent;
            this.dataSource = dataSource;
        }

        @SuppressWarnings("unchecked")
        static WrfRisk from(Map<String, Object> result) {
            Map<String, Object> risk = (Map<String, Object>) result.get("risk");
            Map<String, Object> turb = (Map<String, Object>) risk.get("turbulence");
            Map<String, Object> vis = (Map<String, Object>) risk.get("visibility");
            Map<String, Object> conv = (Map<String, Object>) risk.get("convection");
            if (turb == null || vis == null || conv == null) {
                throw new IllegalStateException("risk 块缺少 turbulence/visibility/convection 之一");
            }

            // ── 2 m 温湿：解析器的默认第 0 层就是 2 m（DEFAULT_LEVELS_M[0]=2，
            //    且插值锚点正是 T2/Q2），因此 payload.temperature[0] / payload.rh[0]
            //    的域平均就是 2 m 温湿。取不到就留 null（端点会省略这两个字段），
            //    **不编造**。
            Double t2c = null;
            Double rh2 = null;
            Object payloadObj = result.get("payload");
            if (payloadObj instanceof Map) {
                Map<String, Object> payload = (Map<String, Object>) payloadObj;
                Double t2k = meanOfFirstLevel(payload.get("temperature"));
                rh2 = meanOfFirstLevel(payload.get("rh"));
                if (t2k != null) {
                    t2c = t2k - 273.15;
                }
            }

            Object src = result.get("meta");
            String dataSource = "wrf_processor.py";
            if (src instanceof Map) {
                Object fp = ((Map<String, Object>) src).get("filePath");
                Object t = ((Map<String, Object>) src).get("time");
                dataSource = (fp != null ? String.valueOf(fp) : dataSource)
                        + (t != null ? " @ " + t : "");
            }

            return new WrfRisk(
                    asDouble(turb.get("tke_mean")),
                    asDouble(turb.get("tke_max")),
                    asDouble(turb.get("tke_min")),
                    asDouble(turb.get("dissipation_rate")),
                    asDouble(vis.get("visibility_min_m")),
                    asDouble(vis.get("visibility_mean_m")),
                    asDouble(vis.get("extinction_coefficient_mean")),
                    asDouble(conv.get("cape_max")),
                    asDouble(conv.get("cape_mean")),
                    asDouble(conv.get("cin_min")),
                    asDouble(conv.get("lcl_mean_m")),
                    asDouble(conv.get("lfc_mean_m")),
                    t2c, rh2, dataSource);
        }

        /** 3-D 场（[{level}][lat][lon]）第 0 层的域平均。 */
        @SuppressWarnings("unchecked")
        private static Double meanOfFirstLevel(Object field) {
            if (!(field instanceof List)) {
                return null;
            }
            List<Object> levels = (List<Object>) field;
            if (levels.isEmpty() || !(levels.get(0) instanceof List)) {
                return null;
            }
            double sum = 0.0;
            long n = 0;
            for (Object rowObj : (List<Object>) levels.get(0)) {
                if (!(rowObj instanceof List)) {
                    continue;
                }
                for (Object v : (List<Object>) rowObj) {
                    if (v instanceof Number) {
                        sum += ((Number) v).doubleValue();
                        n++;
                    }
                }
            }
            return n == 0 ? null : sum / n;
        }

        private static Double asDouble(Object v) {
            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                return Double.isFinite(d) ? d : null;
            }
            return null;
        }
    }

    public Map<String, Object> getHeightLayers(String fileId, List<Integer> layers) {
        Optional<WrfDataFile> optional = findByFileId(fileId);
        if (optional.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        List<Map<String, Object>> layerDataList = new ArrayList<>();
        for (int h : layers) {
            Map<String, Object> layer = new HashMap<>();
            layer.put("height", h);
            double heightFactor = 1 + 0.3 * Math.log((double) h / 100 + 1);
            if (h == 0) heightFactor = 0.1;
            layer.put("wind_speed", Math.round(8.5 * heightFactor * 100.0) / 100.0);
            layer.put("wind_direction", Math.round((180 + Math.random() * 90) * 10.0) / 10.0);
            layer.put("temperature", Math.round((25.0 - 0.0065 * h) * 10.0) / 10.0);
            layer.put("humidity", Math.round(Math.max(10, 65 - 0.002 * h) * 10.0) / 10.0);
            double tke = Math.max(0, 1.2 * Math.exp(-(double) h / 500));
            layer.put("turbulence_tke", Math.round(tke * 10000.0) / 10000.0);
            if (tke >= 3.0) layer.put("turbulence_intensity", "SEVERE");
            else if (tke >= 1.5) layer.put("turbulence_intensity", "MODERATE");
            else if (tke >= 0.5) layer.put("turbulence_intensity", "LOW");
            else layer.put("turbulence_intensity", "NEGLIGIBLE");
            layerDataList.add(layer);
        }
        return Map.of(
            "success", true,
            "data", Map.of("fileId", fileId, "layers", layerDataList, "layer_count", layers.size(), "timestamp", System.currentTimeMillis())
        );
    }

    public Map<String, Object> getStatistics(String fileId) {
        Optional<WrfDataFile> optionalWrfDataFile = findByFileId(fileId);
        if (optionalWrfDataFile.isEmpty()) {
            return Map.of("success", false, "message", "File not found: " + fileId);
        }
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("fileId", fileId);
        // ⚠️ 诚实标注：本端点的六个统计块**仍是硬编码占位值**（不属于 C2 的三项，
        //    故本轮未接线）。打上显式标记，避免它像三个诊断端点那样"看起来是通的"。
        statistics.put("placeholder", true);
        statistics.put("placeholderNote", "本端点统计量尚未接入真实 wrfout，"
                + "数值为占位常量；/turbulence 与 /visibility 已接入真实数据");
        statistics.put("wind_speed", Map.of("min", 5.2, "max", 14.8, "mean", 9.5, "std", 2.3));
        statistics.put("temperature", Map.of("min", 15.1, "max", 29.8, "mean", 22.3, "std", 3.1));
        statistics.put("humidity", Map.of("min", 41.2, "max", 79.5, "mean", 60.8, "std", 8.2));
        statistics.put("turbulence_tke", Map.of("min", 0.3, "max", 2.8, "mean", 1.2, "std", 0.6));
        statistics.put("visibility_m", Map.of("min", 5000, "max", 20000, "mean", 12000, "std", 3500));
        statistics.put("lightning_risk_score", Map.of("min", 5, "max", 75, "mean", 28, "std", 15));
        return Map.of("success", true, "data", statistics);
    }

    public Map<String, Object> getDetail(Long id) {
        Optional<WrfDataFile> optionalWrfDataFile = findById(id);
        if (optionalWrfDataFile.isEmpty()) {
            return Map.of("success", false, "message", "Record not found with id: " + id);
        }
        WrfDataFile wrfDataFile = optionalWrfDataFile.get();
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", wrfDataFile.getId());
        detail.put("fileId", wrfDataFile.getFileId());
        detail.put("fileName", wrfDataFile.getFileName());
        detail.put("fileSize", wrfDataFile.getFileSize());
        detail.put("height", wrfDataFile.getHeight());
        detail.put("timeSteps", wrfDataFile.getTimeSteps());
        detail.put("variables", wrfDataFile.getVariables() != null
            ? Arrays.asList(wrfDataFile.getVariables().split(","))
            : List.of());
        detail.put("status", wrfDataFile.getStatus());
        detail.put("createdAt", wrfDataFile.getCreatedAt() != null ? wrfDataFile.getCreatedAt().toString() : null);
        detail.put("updatedAt", wrfDataFile.getUpdatedAt() != null ? wrfDataFile.getUpdatedAt().toString() : null);
        return Map.of("success", true, "data", detail);
    }

    private double[][] generateGridData(int rows, int cols, double min, double max) {
        double[][] grid = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = min + Math.random() * (max - min);
            }
        }
        return grid;
    }
}
