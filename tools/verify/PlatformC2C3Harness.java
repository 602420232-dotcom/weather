import com.fasterxml.jackson.databind.ObjectMapper;
import com.uav.common.script.PythonScriptInvoker;
import com.uav.wrf.processor.controller.WrfController;
import com.uav.wrf.processor.entity.WrfDataFile;
import com.uav.wrf.processor.repository.WrfDataFileRepository;
import com.uav.wrf.processor.service.WrfChunkUploadService;
import com.uav.wrf.processor.service.WrfDataService;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * C1/C2/C3 的平台侧验证 harness（**不入 src**，放在 target/_verify 下）。
 *
 * 它做的是：把**生产代码原样**装配起来跑一遍 —— 不 mock Spring、不 mock 业务逻辑，
 * 只把两处外部世界替换掉：
 *   1. JPA 仓库 → 一个 java.lang.reflect.Proxy 桩（只回答 findByFileId / save）；
 *   2. Python 解释器 → 由 PATH 注入的 shim（真解释器，带 netCDF4）。
 * 其余（WrfController / WrfDataService / WrfChunkUploadService / PythonScriptInvoker /
 * wrf_processor.py / 真实 wrfout）全部是真的。
 */
public final class PlatformC2C3Harness {

    static final String FILE_ID = "harness_file";
    static Path wrfout;
    static Path dataDir;
    static ObjectMapper M = new ObjectMapper();
    static StringBuilder LOG = new StringBuilder();
    static int pass = 0;
    static int fail = 0;

    public static void main(String[] args) throws Exception {
        wrfout = Paths.get(args[0]).toAbsolutePath();
        dataDir = Paths.get(args[1]).toAbsolutePath();
        Path pyDir = Paths.get(args[2]).toAbsolutePath();
        OUT_FILE = Paths.get(args[3]).toAbsolutePath();
        Files.deleteIfExists(OUT_FILE);
        Files.createDirectories(dataDir);
        out("# harness start " + java.time.LocalDateTime.now());

        check("0.1 真实 wrfout 存在", Files.isRegularFile(wrfout), wrfout.toString());
        long wrfBytes = Files.size(wrfout);
        String wrfSha = WrfChunkUploadService.sha256Hex(wrfout);
        out("== 输入文件 ==");
        out("  path   = " + wrfout);
        out("  bytes  = " + wrfBytes);
        out("  sha256 = " + wrfSha);

        // ── 装配：真 WrfDataService + 桩仓库 + 真 PythonScriptInvoker ──
        WrfDataFile rec = WrfDataFile.builder()
                .id(1L).fileId(FILE_ID).fileName(wrfout.getFileName().toString())
                .filePath(wrfout.toString()).fileSize(wrfBytes).height(100)
                .timeSteps(1).variables("T,U,V,W,RH,WS")
                .status("UPLOADED").build();
        List<WrfDataFile> store = new ArrayList<>();
        store.add(rec);

        WrfDataFileRepository repo = (WrfDataFileRepository) Proxy.newProxyInstance(
                PlatformC2C3Harness.class.getClassLoader(),
                new Class<?>[]{WrfDataFileRepository.class},
                (proxy, method, margs) -> {
                    switch (method.getName()) {
                        case "findByFileId":
                            for (WrfDataFile f : store) {
                                if (f.getFileId().equals(margs[0])) return Optional.of(f);
                            }
                            return Optional.empty();
                        case "save":
                            store.add((WrfDataFile) margs[0]);
                            return margs[0];
                        case "findById":
                            return Optional.of(rec);
                        case "toString":
                            return "StubWrfDataFileRepository";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == margs[0];
                        default:
                            return null;
                    }
                });

        ObjectMapper om = new ObjectMapper();
        PythonScriptInvoker invoker = new PythonScriptInvoker(om);
        set(invoker, "scriptPath", pyDir.toString());
        set(invoker, "timeout", 300000);
        set(invoker, "enabled", true);
        set(invoker, "hashingEnabled", true);
        // ★ 2026-09-28：显式解释器（第 5 个参数，可选）。自动探测只看 `--version`，
        //   不查 numpy/netCDF4 ⇒ 本机 PATH 里第一个 `python3`（MSYS 3.14）没有依赖。
        if (args.length > 4 && !args[4].isBlank()) {
            set(invoker, "pythonExecutable", args[4]);
        }

        WrfDataService service = new WrfDataService(repo, invoker);
        set(service, "pythonScriptPath", "wrf_processor.py");

        WrfChunkUploadService chunkService = new WrfChunkUploadService();
        set(chunkService, "dataPath", dataDir.toString());

        WrfController controller = new WrfController(service, invoker, chunkService);
        set(controller, "pythonScriptPath", "wrf_processor.py");
        set(controller, "dataPath", dataDir.toString());

        // ═══════════════════════════════════════════════════════════════
        //  C2：三个诊断端点
        // ═══════════════════════════════════════════════════════════════
        out("");
        out("================================================================");
        out(" C2  /turbulence  /visibility  /lightning-risk   真实输入 → 输出");
        out("================================================================");

        Map<String, Object> turb = controller.getTurbulence(FILE_ID);
        out("");
        out("--- GET /api/wrf/turbulence?fileId=" + FILE_ID + " ---");
        out(pretty(turb));
        check("C2.1 turbulence success=true", Boolean.TRUE.equals(turb.get("success")), null);
        Map<String, Object> td = data(turb);
        check("C2.2 turbulence 的 tke_mean 不再是硬编码 1.2",
                td != null && !Double.valueOf(1.2).equals(asD(td.get("tke_mean"))),
                "tke_mean=" + (td == null ? null : td.get("tke_mean")));
        check("C2.3 turbulence 的 tke_max 不再是硬编码 2.8",
                td != null && !Double.valueOf(2.8).equals(asD(td.get("tke_max"))),
                "tke_max=" + (td == null ? null : td.get("tke_max")));
        check("C2.4 turbulence 带 method/caveat（可追溯来源）",
                td != null && td.containsKey("method") && td.containsKey("caveat"), null);

        Map<String, Object> vis = controller.getVisibility(FILE_ID);
        out("");
        out("--- GET /api/wrf/visibility?fileId=" + FILE_ID + " ---");
        out(pretty(vis));
        check("C2.5 visibility success=true", Boolean.TRUE.equals(vis.get("success")), null);
        Map<String, Object> vd = data(vis);
        check("C2.6 visibility 的 visibility_m 不再是硬编码 8500.0",
                vd != null && !Double.valueOf(8500.0).equals(asD(vd.get("visibility_m"))),
                "visibility_m=" + (vd == null ? null : vd.get("visibility_m")));
        check("C2.7 visibility 的 extinction_coefficient 不再是硬编码 0.46",
                vd != null && !Double.valueOf(0.46).equals(asD(vd.get("extinction_coefficient"))),
                "extinction=" + (vd == null ? null : vd.get("extinction_coefficient")));

        Map<String, Object> lr = controller.getLightningRisk(FILE_ID);
        out("");
        out("--- GET /api/wrf/lightning-risk?fileId=" + FILE_ID + " ---");
        out(pretty(lr));
        check("C2.8 lightning success=true", Boolean.TRUE.equals(lr.get("success")), null);
        Map<String, Object> ld = data(lr);
        check("C2.9 lightning 的 cape 不再是硬编码 680.0",
                ld != null && !Double.valueOf(680.0).equals(asD(ld.get("cape"))),
                "cape=" + (ld == null ? null : ld.get("cape")));
        check("C2.10 lightning 的 risk_score 不再是硬编码 28.5",
                ld != null && !Double.valueOf(28.5).equals(asD(ld.get("risk_score"))),
                "risk_score=" + (ld == null ? null : ld.get("risk_score")));

        // 解析失败时**不得**回退到常量
        Map<String, Object> missing = controller.getTurbulence("no_such_file");
        check("C2.11 文件不存在时 success=false（不编造数据）",
                Boolean.FALSE.equals(missing.get("success")), String.valueOf(missing));

        // ═══════════════════════════════════════════════════════════════
        //  C3：分片上传三个端点 + parse-uploaded
        // ═══════════════════════════════════════════════════════════════
        out("");
        out("================================================================");
        out(" C3  /upload/chunk  /upload/status  /upload/merge  /parse-uploaded");
        out("================================================================");

        byte[] all = Files.readAllBytes(wrfout);
        int CHUNK = 5 * 1024 * 1024;
        int total = (int) Math.ceil(all.length / (double) CHUNK);
        String fileKey = "harnessKey0001";
        String fileName = "wrfout_harness.nc";
        Map<Integer, String> hashes = new LinkedHashMap<>();

        out("  chunk size = " + CHUNK + " B, totalChunks = " + total);

        // 先按"断点续传"的方式验证：只传前 2 片 → status 应报不完整
        int pre = Math.min(2, total);
        for (int i = 0; i < pre; i++) {
            byte[] part = slice(all, i, CHUNK);
            String h = WrfChunkUploadService.sha256Hex(part);
            hashes.put(i, h);
            Map<String, Object> r = controller.uploadChunk(
                    new MockMultipartFile("file", fileName, "application/octet-stream", part),
                    i, total, fileKey, fileName, h);
            check("C3.1 分片 " + i + " 落盘成功 (hashVerified=" + r.get("hashVerified") + ")",
                    Boolean.TRUE.equals(r.get("success")), null);
        }
        Map<String, Object> st1 = controller.uploadStatus(fileKey, total);
        out("");
        out("--- GET /api/wrf/upload/status?fileKey=" + fileKey + "&totalChunks=" + total + " ---");
        out(pretty(st1));
        check("C3.2 status 能从磁盘列出已收到的分片",
                st1.get("receivedCount") != null && ((Number) st1.get("receivedCount")).intValue() == pre,
                "receivedCount=" + st1.get("receivedCount"));
        check("C3.3 status 正确报告不完整 + 缺哪几片",
                Boolean.FALSE.equals(st1.get("complete"))
                        && ((List<?>) st1.get("missingChunks")).size() == total - pre,
                "missing=" + st1.get("missingChunks"));

        // 合并应被拒绝（分片不齐）
        Map<String, Object> badMerge = controller.mergeChunks(Map.of(
                "fileKey", fileKey, "fileName", fileName, "totalChunks", total));
        check("C3.4 分片不齐时 merge 被拒绝",
                Boolean.FALSE.equals(badMerge.get("success")),
                String.valueOf(badMerge.get("error")));

        // 续传剩下的分片
        for (int i = pre; i < total; i++) {
            byte[] part = slice(all, i, CHUNK);
            String h = WrfChunkUploadService.sha256Hex(part);
            hashes.put(i, h);
            Map<String, Object> r = controller.uploadChunk(
                    new MockMultipartFile("file", fileName, "application/octet-stream", part),
                    i, total, fileKey, fileName, h);
            check("C3.5 续传分片 " + i + " 成功", Boolean.TRUE.equals(r.get("success")), null);
        }
        Map<String, Object> st2 = controller.uploadStatus(fileKey, total);
        check("C3.6 全部到齐后 complete=true", Boolean.TRUE.equals(st2.get("complete")),
                "receivedCount=" + st2.get("receivedCount"));

        Map<String, Object> hashReq = new LinkedHashMap<>();
        hashReq.put("fileKey", fileKey);
        hashReq.put("fileName", fileName);
        hashReq.put("totalChunks", total);
        Map<String, String> hashMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> e : hashes.entrySet()) {
            hashMap.put(String.valueOf(e.getKey()), e.getValue());
        }
        hashReq.put("chunkHashes", hashMap);
        Map<String, Object> merge = controller.mergeChunks(hashReq);
        out("");
        out("--- POST /api/wrf/upload/merge ---");
        out(pretty(merge));
        check("C3.7 merge 成功", Boolean.TRUE.equals(merge.get("success")),
                String.valueOf(merge.get("error")));
        Map<String, Object> md = data(merge);
        String mergedSha = md == null ? null : String.valueOf(md.get("sha256"));
        check("C3.8 ★ 合并产物 sha256 == 原始 wrfout sha256（逐位一致）",
                wrfSha.equalsIgnoreCase(mergedSha), "merged=" + mergedSha + " orig=" + wrfSha);
        check("C3.9 合并后分片目录已清理（不留等量垃圾）",
                chunkService.receivedChunks(fileKey, total).isEmpty(), null);

        String uploadedFileId = md == null ? null : String.valueOf(md.get("fileId"));
        out("  → 合并登记 fileId = " + uploadedFileId);

        Map<String, Object> parsed = controller.parseUploaded(Map.of("fileId", uploadedFileId));
        out("");
        out("--- POST /api/wrf/parse-uploaded {fileId=" + uploadedFileId + "} ---");
        Map<String, Object> shown = new LinkedHashMap<>();
        shown.put("success", parsed.get("success"));
        shown.put("fileId", parsed.get("fileId"));
        String raw = parsed.get("data") == null ? null : String.valueOf(parsed.get("data"));
        shown.put("data(stdout 原始返回) 长度", raw == null ? null : raw.length());
        if (raw != null) {
            Map<?, ?> p = om.readValue(raw, Map.class);
            shown.put("data.success", p.get("success"));
            shown.put("data.payload.dims", ((Map<?, ?>) p.get("payload")).get("dims"));
            shown.put("data.meta.physics", ((Map<?, ?>) p.get("meta")).get("physics"));
        }
        out(pretty(shown));
        check("C3.10 parse-uploaded 能按 fileId 解析服务端已持有的文件",
                Boolean.TRUE.equals(parsed.get("success")), null);

        // ═══════════════════════════════════════════════════════════════
        //  C1：/parse（前端 parseViaBackend 打的就是这个端点）
        // ═══════════════════════════════════════════════════════════════
        out("");
        out("================================================================");
        out(" C1  POST /api/wrf/parse  （前端 parseViaBackend() 的目标端点）");
        out("================================================================");
        Map<String, Object> parseResp = controller.parseWrfFile(
                new MockMultipartFile("file", "wrfout_d01_2026-09-20_00_00_00.nc",
                        "application/octet-stream", all), 100);
        check("C1.1 /parse success=true", Boolean.TRUE.equals(parseResp.get("success")),
                String.valueOf(parseResp.get("error")));
        check("C1.2 /parse 返回了 fileId", parseResp.get("fileId") != null, null);
        String data2 = parseResp.get("data") == null ? null : String.valueOf(parseResp.get("data"));
        check("C1.3 /parse 的 data 是 Python stdout 原始字符串（前端要 JSON.parse 一次）",
                data2 != null && data2.trim().startsWith("{"), null);
        if (data2 != null) {
            Map<?, ?> pd = om.readValue(data2, Map.class);
            Map<?, ?> payload = (Map<?, ?>) pd.get("payload");
            out("  payload.dims          = " + payload.get("dims"));
            out("  payload.vars          = " + payload.get("vars"));
            out("  payload.levelNames[0] = "
                    + ((List<?>) payload.get("levelNames")).get(0));
            out("  success               = " + pd.get("success"));
            check("C1.4 payload 形状满足前端契约（dims/levelNames/temperature/u/v）",
                    payload.get("dims") != null && payload.get("levelNames") != null
                            && payload.get("temperature") != null
                            && payload.get("u") != null && payload.get("v") != null, null);
            check("C1.5 ★ 不是 mock：payload 第一层温度是真实的 K 值域（不是 290 恒定/随机）",
                    temperatureLooksReal(payload), null);
        }

        // ── 汇总 ──
        out("");
        out("================================================================");
        out(" 判据汇总：PASS " + pass + " / FAIL " + fail);
        out("================================================================");
        Files.writeString(Paths.get(args[3]), LOG.toString());
        System.out.println(LOG);
        if (fail > 0) {
            System.exit(1);
        }
    }

    // ──────────────────────────────────────────────────────────── helpers

    @SuppressWarnings("unchecked")
    static boolean temperatureLooksReal(Map<?, ?> payload) {
        List<?> t = (List<?>) payload.get("temperature");
        List<?> row = (List<?>) t.get(0);
        List<?> vals = (List<?>) row.get(0);
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;
        int n = 0;
        for (Object o : vals) {
            double d = ((Number) o).doubleValue();
            min = Math.min(min, d);
            max = Math.max(max, d);
            sum += d;
            n++;
        }
        double mean = sum / n;
        out(String.format("  首行温度：n=%d min=%.2f mean=%.2f max=%.2f K", n, min, mean, max));
        // 真实 2 m 温度场：在 230~320 K 之间，且**不是常数**、**不是整数格点**
        return n > 1 && min > 230.0 && max < 320.0 && (max - min) > 0.5 && mean != Math.rint(mean);
    }

    static byte[] slice(byte[] all, int i, int size) {
        int start = i * size;
        int end = Math.min(start + size, all.length);
        byte[] out = new byte[end - start];
        System.arraycopy(all, start, out, 0, out.length);
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> data(Map<String, Object> resp) {
        Object d = resp == null ? null : resp.get("data");
        return d instanceof Map ? (Map<String, Object>) d : null;
    }

    static Double asD(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : null;
    }

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
        } else {
            fail++;
        }
        out((ok ? "  [PASS] " : "  [FAIL] ") + name + (detail == null ? "" : "   → " + detail));
    }

    static void out(String s) {
        LOG.append(s).append(System.lineSeparator());
        // 立刻落盘：跑挂时也能看到走到哪一步
        try {
            if (OUT_FILE != null) {
                Files.writeString(OUT_FILE, s + System.lineSeparator(),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (Exception ignored) {
            // 诊断输出失败不影响判据
        }
    }

    static Path OUT_FILE = null;

    static String pretty(Object o) {
        try {
            return M.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    static {
        // 让 Jackson 打印中文不转义
        M = new ObjectMapper();
    }
}
