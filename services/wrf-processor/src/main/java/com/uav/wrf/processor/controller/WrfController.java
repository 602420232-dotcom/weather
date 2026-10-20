package com.uav.wrf.processor.controller;

import com.uav.common.dto.WrfParseRequest;
import com.uav.common.script.PythonScriptInvoker;
import com.uav.wrf.processor.entity.WrfDataFile;
import com.uav.wrf.processor.service.WrfChunkUploadService;
import com.uav.wrf.processor.service.WrfDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/wrf")
@Slf4j
@RequiredArgsConstructor
public class WrfController {

    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 30000;

    /**
     * ★★ 2026-10-01 修：默认高度层与 Python 侧**对齐**。
     *
     * <p>修复前这里有**三处互相不一致的默认层**（实测查清）：
     * <ul>
     *   <li>本常量 = <b>9 层</b> {@code [0,10,50,100,200,300,500,800,1000]}（{@code /height-layers} 用）；</li>
     *   <li>Python {@code wrf_processor.DEFAULT_LEVELS_M} = <b>12 层</b>
     *       {@code [2,10,50,100,200,300,500,800,1000,1500,2000,3000]}（不给参数时用）；</li>
     *   <li>前端 {@code parseViaBackend()} 硬编码 {@code ?height=100} ⇒ <b>1 层</b>。</li>
     * </ul>
     * ⇒ 同一个产品里"高度层"有三个答案。现统一到 Python 那 12 层（它的名字就叫
     * {@code DEFAULT_LEVELS_M}，是这一维的事实定义处），并让前端改传一组层。
     *
     * <p>注意 Python 的 {@code levels} **优先于** {@code height} ⇒ 传了 {@code levels}
     * 就不会再被"单个 height 撑成 1 层"。
     */
    private static final List<Integer> DEFAULT_HEIGHT_LAYERS =
            List.of(2, 10, 50, 100, 200, 300, 500, 800, 1000, 1500, 2000, 3000);

    private static final int MAX_HEIGHT_LAYERS = 50;

    @Value("${wrf.python-script:wrf_processor.py}")
    private String pythonScriptPath;

    @Value("${wrf.data-path:./data}")
    private String dataPath;

    private final WrfDataService wrfDataService;
    private final PythonScriptInvoker pythonScriptInvoker;
    /** ★ C3 新增：分片上传的落盘/合并。 */
    private final WrfChunkUploadService chunkUploadService;

    @PostMapping(value = "/parse-params", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> parseWrfData(@RequestBody WrfParseRequest request) {
        try {
            log.info("Processing parameterized WRF data, height={}", request.getSafeHeight());

            int height = request.getSafeHeight();
            if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
                return Map.of(
                    "success", false, "code", 400,
                    "message", "高度值必须在" + MIN_HEIGHT + "到" + MAX_HEIGHT + "之间"
                );
            }

            if (request.getFilePath() != null && !request.getFilePath().isBlank()) {
                return processWrfFromFilePath(request.getFilePath(), height, null);
            }

            if (request.getData() != null && !request.getData().isEmpty()) {
                return Map.of(
                    "success", true, "code", 200,
                    "message", "WRF数据解析成功", "data", request.getData()
                );
            }

            Map<String, Object> result = generateMockWeatherData(height, request.getBounds());
            return Map.of(
                "success", true, "code", 200,
                "message", "WRF数据解析成功", "data", result.get("data")
            );
        } catch (Exception e) {
            log.error("Failed to process parameterized WRF data", e);
            return Map.of("success", false, "code", 500, "message", "处理失败: " + e.getMessage());
        }
    }

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> parseWrfFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "height", defaultValue = "100") int height,
            //   ★★ 2026-10-01 新增：`levels` —— 逗号分隔的一组高度（m AGL）。
            //     为什么必须加：修复前只传 `height=100`，而 Python 把标量转成
            //     `levels=[100.0]` ⇒ **只返回 1 层**，页面高度层滑块只剩一格。
            //     Python 侧 `levels` **优先于** `height`（`wrf_processor.py:1184-1189`），
            //     所以只要这里带上 `levels`，多层就通了 —— **不需要改 Python**。
            @RequestParam(value = "levels", required = false) String levelsParam) {
        Path tempFile = null;
        boolean registered = false;
        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                return Map.of("success", false, "error", "文件名不能为空");
            }
            if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
                return Map.of("success", false, "error", "文件名包含非法字符");
            }
            if (!originalName.endsWith(".nc") && !originalName.endsWith(".netcdf")) {
                return Map.of("success", false, "error", "仅支持NetCDF格式文件");
            }
            if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
                return Map.of("success", false, "error",
                    "高度值必须在" + MIN_HEIGHT + "到" + MAX_HEIGHT + "之间");
            }
            //   解析 layers 参数（与 /height-layers 同一套白名单规则：
            //   0~30000 之间的整数，最多 MAX_HEIGHT_LAYERS 个）。
            //   ⚠️ 解析失败**不静默回退**到默认层 —— 那会让"传了但没生效"变成看不见的问题
            //      （这正是 Python 侧 FIX-HEIGHT 注释里记的那类事故）。
            List<Integer> levels = null;
            if (levelsParam != null && !levelsParam.isBlank()) {
                List<Integer> parsed = new ArrayList<>();
                for (String part : levelsParam.split(",")) {
                    String s = part.trim();
                    if (s.isEmpty()) {
                        continue;
                    }
                    int h;
                    try {
                        h = Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return Map.of("success", false, "error",
                            "levels 参数无法解析为整数：" + s);
                    }
                    if (h < 0 || h > MAX_HEIGHT) {
                        return Map.of("success", false, "error",
                            "levels 中的高度超出 0~" + MAX_HEIGHT + "：" + h);
                    }
                    parsed.add(h);
                }
                if (parsed.isEmpty()) {
                    return Map.of("success", false, "error", "levels 参数为空");
                }
                if (parsed.size() > MAX_HEIGHT_LAYERS) {
                    return Map.of("success", false, "error",
                        "levels 最多 " + MAX_HEIGHT_LAYERS + " 个，收到 " + parsed.size());
                }
                levels = parsed;
            }

            String safeName = UUID.randomUUID().toString() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            // ★ 2026-09-28 修：`wrf.data-path`（生产是相对路径 `./data`）**先绝对化**。
            //   相对路径在这里有两个解释器：`Files.createDirectories` 按 JVM 工作目录，
            //   而 Undertow 的 `Part.write`（Spring `transferTo` 的底层）按 **Servlet 规范的
            //   multipart 临时目录** ⇒ 两边指向不同位置，实测浏览器 E2E 直接抛
            //   `NoSuchFileException: <java.io.tmpdir>/undertow.8089.…/target/e2e-data/…`。
            //   绝对化之后两者一致；同时入库的 filePath 也不再依赖"谁来解析相对路径"。
            Path root = dataRoot();
            tempFile = root.resolve(safeName).normalize();

            if (!tempFile.startsWith(root)) {
                return Map.of("success", false, "error", "路径遍历攻击检测");
            }

            // ★ 2026-09-28 修：**必须在 transferTo 之前**取大小。
            //   本服务跑在 Undertow 上，而 Undertow 的 Part 把上传内容落在临时文件里；
            //   `transferTo()` 之后再去问 `file.getSize()`（PartImpl.getSize →
            //   FormData.FileItem.getFileSize → Files.size(临时文件)）会抛
            //   `NoSuchFileException: <temp>/undertow.NNN/undertowNNNupload`
            //   ⇒ 整个 /parse 变成 500。
            //   实测：只有当文件超过"内存阈值"、真正落过盘时才会触发 ——
            //   所以 1 MB 的 multipart 上限一放开，这个缺陷立刻显形
            //   （放开上限之前请求在更早的地方就被 1 MB 拦掉了）。
            long uploadedSize = file.getSize();

            Files.createDirectories(tempFile.getParent());
            java.io.File targetFile = tempFile.toFile();
            if (targetFile != null) {
                file.transferTo(targetFile);
            }

            // Validate script path for security
            if (pythonScriptPath == null || pythonScriptPath.isBlank()) {
                return Map.of("success", false, "error", "安全验证失败");
            }
            if (pythonScriptPath.contains("..")) {
                return Map.of("success", false, "error", "安全验证失败");
            }
            String scriptName = Paths.get(pythonScriptPath).normalize().getFileName().toString();
            if (!scriptName.equals("wrf_processor.py")) {
                return Map.of("success", false, "error", "安全验证失败");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("filePath", tempFile.toString());
            params.put("height", height);
            //   ★ 只有显式给了 levels 才放进 params —— 让 Python 侧"没给就用它的
            //     默认 12 层"这条既有语义保持原样（也便于与历史行为对照）。
            if (levels != null) {
                params.put("levels", levels);
            }
            String result = pythonScriptInvoker.execute(pythonScriptPath, "parse", params);

            WrfDataFile savedFile = wrfDataService.createWrfDataFile(originalName, tempFile.toString(), uploadedSize);
            registered = true;

            return Map.of("success", true, "data", result, "fileId", savedFile.getFileId());
        } catch (Exception e) {
            log.error("处理失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "处理失败: " + e.getClass().getSimpleName());
        } finally {
            // ★ 2026-09-28 修：**不再删除已经登记入库的文件**。
            //
            //   原实现无论成败都 `deleteIfExists(tempFile)`，可成功分支里
            //   `createWrfDataFile(originalName, tempFile.toString(), size)` 已经把
            //   这个路径写进了 fileId 记录的 `filePath`
            //   ⇒ 记录一入库就指向一个**已被删掉**的文件。后果是：
            //   凡「按 fileId 取数」的端点（`/turbulence`、`/visibility`、
            //   `/lightning-risk`、`/height-layers`、`/data`、`/stats`）对
            //   **经 `/parse` 上传**的文件全部失效（实测：拿到 fileId 后调
            //   `/turbulence` 一律返回「解析器未返回 risk 块」——因为文件不存在了）。
            //   `/parse-uploaded`（分片合并后按 fileId 解析）本来就是按「服务端持久
            //   持有该文件」设计的，两条路径的行为原本自相矛盾。
            //
            //   现在：只在「没能登记成功」时才删，避免留孤儿文件；
            //   登记成功的那份就是数据仓里的正式副本（与分片合并产物同一目录）。
            if (tempFile != null && !registered) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            } else if (tempFile != null) {
                log.info("已保留上传文件供 fileId 端点复用: {}", tempFile);
            }
        }
    }

    @PostMapping("/parse/advanced")
    public Map<String, Object> parseWrfAdvanced(@RequestBody Map<String, Object> request) {
        try {
            String filePath = (String) request.get("filePath");
            Integer height = request.get("height") != null ? (Integer) request.get("height") : 100;
            String paramType = (String) request.get("paramType");

            if (filePath == null || filePath.isBlank()) {
                return Map.of("success", false, "error", "文件路径不能为空");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("filePath", filePath);
            params.put("height", height);
            params.put("paramType", paramType != null ? paramType : "all");
            String result = pythonScriptInvoker.execute(pythonScriptPath, "parse", params);

            return Map.of("success", true, "data", result);
        } catch (Exception e) {
            log.error("高级解析失败", e);
            return Map.of("success", false, "error", "解析失败: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ★ C3：分片上传（前端 NetCDFChunkUploader.vue 的 realUploadChunk 真正要打的端点）
    //
    //  修复前：后端**没有**任何分片端点，前端 realUploadChunk() 组装好 FormData
    //  之后只 await sleep()，从不发送。现在三个端点构成一条可断点续传的完整链路：
    //      POST /upload/chunk      逐片落盘
    //      GET  /upload/status     问「已收到哪几片」（答案来自磁盘目录，不是内存）
    //      POST /upload/merge      校验齐全 + 逐片 SHA-256 复核 + 合并 + 登记 fileId
    //      DELETE /upload/chunk    取消上传，清掉分片垃圾
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping(value = "/upload/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadChunk(@RequestPart("file") MultipartFile chunk,
                                           @RequestParam("chunkIndex") int chunkIndex,
                                           @RequestParam("totalChunks") int totalChunks,
                                           @RequestParam("fileKey") String fileKey,
                                           @RequestParam("fileName") String fileName,
                                           @RequestParam(value = "chunkHash", required = false) String chunkHash) {
        log.info("Receiving chunk {}/{} of {} (fileKey={})",
                chunkIndex + 1, totalChunks, fileName, fileKey);
        try {
            return chunkUploadService.saveChunk(
                    fileKey, fileName, chunkIndex, totalChunks, chunkHash, chunk);
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "error", e.getMessage());
        } catch (Exception e) {
            log.error("分片落盘失败 fileKey={} index={}", fileKey, chunkIndex, e);
            return Map.of("success", false, "error", "分片落盘失败: " + e.getClass().getSimpleName());
        }
    }

    @GetMapping("/upload/status")
    public Map<String, Object> uploadStatus(@RequestParam("fileKey") String fileKey,
                                            @RequestParam(value = "totalChunks", required = false) Integer totalChunks) {
        try {
            return chunkUploadService.status(fileKey, totalChunks);
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "error", e.getMessage());
        } catch (Exception e) {
            log.error("查询分片状态失败 fileKey={}", fileKey, e);
            return Map.of("success", false, "error", "查询分片状态失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片。请求体：
     * <pre>
     * { "fileKey": "...", "fileName": "wrfout_d01_...nc", "totalChunks": 4,
     *   "chunkHashes": { "0": "ab12…", "1": "cd34…" } }   // chunkHashes 可选
     * </pre>
     * 成功后返回 {@code fileId} —— 前端拿它去调 {@code POST /parse-uploaded}，
     * 避免把同一份文件再整体传一遍。
     */
    @PostMapping(value = "/upload/merge", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> mergeChunks(@RequestBody Map<String, Object> request) {
        try {
            String fileKey = asString(request.get("fileKey"));
            String fileName = asString(request.get("fileName"));
            Object tc = request.get("totalChunks");
            if (tc == null) {
                return Map.of("success", false, "error", "缺少 totalChunks");
            }
            int totalChunks = Integer.parseInt(tc.toString());

            Map<Integer, String> hashes = new HashMap<>();
            Object rawHashes = request.get("chunkHashes");
            if (rawHashes instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) rawHashes).entrySet()) {
                    try {
                        hashes.put(Integer.parseInt(String.valueOf(e.getKey())),
                                e.getValue() == null ? null : String.valueOf(e.getValue()));
                    } catch (NumberFormatException ignored) {
                        // 非数字键忽略
                    }
                }
            }

            WrfChunkUploadService.MergeResult merged =
                    chunkUploadService.merge(fileKey, fileName, totalChunks, hashes);
            WrfDataFile saved = chunkUploadService.register(wrfDataService, merged, fileName);

            log.info("Merged {} chunks -> {} ({} bytes, sha256={})",
                    totalChunks, merged.path, merged.bytes, merged.sha256);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("fileId", saved.getFileId());
            data.put("id", saved.getId());
            data.put("fileName", saved.getFileName());
            data.put("fileSize", merged.bytes);
            data.put("sha256", merged.sha256);
            data.put("chunkCount", totalChunks);
            data.put("serverPath", merged.path.toString());
            return Map.of("success", true, "message", "分片合并完成", "data", data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Map.of("success", false, "error", e.getMessage());
        } catch (Exception e) {
            log.error("分片合并失败", e);
            return Map.of("success", false, "error", "分片合并失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/upload/chunk")
    public Map<String, Object> cancelUpload(@RequestParam("fileKey") String fileKey) {
        try {
            int removed = chunkUploadService.deleteChunks(fileKey);
            return Map.of("success", true, "removed", removed);
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "error", e.getMessage());
        } catch (Exception e) {
            log.error("清理分片失败 fileKey={}", fileKey, e);
            return Map.of("success", false, "error", "清理分片失败: " + e.getMessage());
        }
    }

    /**
     * 对<b>服务端已持有</b>的文件（分片合并的产物）按 fileId 解析。
     *
     * <p>与 {@code /parse/advanced} 的区别：那个接受客户端给的 {@code filePath}
     * （任意文件读取面），这个只接受 fileId，路径由服务端查库得到。
     * 返回结构与 {@code POST /parse} 一致，前端复用同一段解析代码。
     */
    @PostMapping(value = "/parse-uploaded", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> parseUploaded(@RequestBody Map<String, Object> request) {
        String fileId = asString(request.get("fileId"));
        if (fileId == null || fileId.isBlank()) {
            return Map.of("success", false, "error", "fileId 不能为空");
        }
        Integer height = null;
        Object h = request.get("height");
        if (h != null) {
            try {
                height = Integer.parseInt(h.toString());
            } catch (NumberFormatException ignored) {
                // 忽略非法高度，走记录里的默认值
            }
        }
        log.info("Parsing previously uploaded file fileId={} height={}", fileId, height);
        return wrfDataService.parseStoredFile(fileId, height);
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    /**
     * ★ 2026-09-28：`wrf.data-path` 的**唯一**解释口径 —— 绝对化 + 归一化。
     *
     * <p>必须绝对化的原因见 {@code parseWrfFile} 里的注释：相对路径在 Java NIO 与
     * Servlet 容器（Undertow `Part.write`）里含义不同，混用会出现"建目录的地方"
     * 与"写文件的地方"不是同一个目录。
     */
    private Path dataRoot() {
        String configured = (dataPath == null || dataPath.isBlank()) ? "." : dataPath;
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    @GetMapping("/turbulence")
    public Map<String, Object> getTurbulence(@RequestParam("fileId") String fileId) {        log.info("Getting turbulence data for fileId: {}", fileId);
        try {
            return wrfDataService.getTurbulence(fileId);
        } catch (Exception e) {
            log.error("获取湍流数据失败", e);
            return Map.of("success", false, "message", "获取湍流数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/visibility")
    public Map<String, Object> getVisibility(@RequestParam("fileId") String fileId) {
        log.info("Getting visibility data for fileId: {}", fileId);
        try {
            return wrfDataService.getVisibility(fileId);
        } catch (Exception e) {
            log.error("获取能见度数据失败", e);
            return Map.of("success", false, "message", "获取能见度数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/lightning-risk")
    public Map<String, Object> getLightningRisk(@RequestParam("fileId") String fileId) {
        log.info("Getting lightning risk for fileId: {}", fileId);
        try {
            return wrfDataService.getLightningRisk(fileId);
        } catch (Exception e) {
            log.error("获取闪电风险评估失败", e);
            return Map.of("success", false, "message", "获取闪电风险评估失败: " + e.getMessage());
        }
    }

    @GetMapping("/height-layers")
    public Map<String, Object> getHeightLayers(
            @RequestParam("fileId") String fileId,
            @RequestParam(value = "layers", required = false) String layersParam) {
        log.info("Getting height layers for fileId: {}", fileId);
        try {
            List<Integer> layers = DEFAULT_HEIGHT_LAYERS;
            if (layersParam != null && !layersParam.isBlank()) {
                String[] parts = layersParam.split(",");
                List<Integer> customLayers = new ArrayList<>();
                for (String part : parts) {
                    try {
                        int h = Integer.parseInt(part.trim());
                        if (h >= 0 && h <= 30000) {
                            customLayers.add(h);
                        }
                    } catch (NumberFormatException e) {
                        log.debug("Invalid height layer value: {}", part, e);
                    }
                }
                if (!customLayers.isEmpty() && customLayers.size() <= MAX_HEIGHT_LAYERS) {
                    layers = customLayers;
                }
            }
            return wrfDataService.getHeightLayers(fileId, layers);
        } catch (Exception e) {
            log.error("获取高度分层数据失败", e);
            return Map.of("success", false, "message", "获取高度分层数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/data")
    public Map<String, Object> getWeatherData(@RequestParam("fileId") String fileId) {
        log.info("Getting weather data for fileId: {}", fileId);
        return wrfDataService.getWeatherData(fileId);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStatistics(@RequestParam("fileId") String fileId) {
        log.info("Getting statistics for fileId: {}", fileId);
        return wrfDataService.getStatistics(fileId);
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadWrfData(@RequestBody Map<String, Object> request) {
        log.info("Uploading WRF data: {}", request);
        try {
            String fileName = (String) request.getOrDefault("fileName", "wrf_file.nc");
            String filePath = (String) request.getOrDefault("filePath", "./data/" + fileName);
            Long fileSize = request.get("fileSize") != null ?
                Long.valueOf(request.get("fileSize").toString()) : 0L;

            WrfDataFile savedFile = wrfDataService.createWrfDataFile(fileName, filePath, fileSize);

            return Map.of(
                "success", true, "message", "WRF数据上传成功",
                "data", Map.of("fileId", savedFile.getFileId(), "id", savedFile.getId())
            );
        } catch (Exception e) {
            log.error("Failed to upload WRF data", e);
            return Map.of("success", false, "message", "上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Map<String, Object> listWrfData(@RequestParam("page") Integer page,
                                           @RequestParam("size") Integer size) {
        log.info("Listing WRF data - page: {}, size: {}", page, size);
        try {
            Page<WrfDataFile> wrfDataPage = wrfDataService.findAll(page, size);
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (WrfDataFile file : wrfDataPage.getContent()) {
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("id", file.getId());
                fileData.put("fileId", file.getFileId());
                fileData.put("fileName", file.getFileName());
                fileData.put("fileSize", file.getFileSize());
                fileData.put("status", file.getStatus());
                fileData.put("createdAt", file.getCreatedAt() != null ? file.getCreatedAt().toString() : null);
                dataList.add(fileData);
            }
            return Map.of(
                "success", true, "data", dataList,
                "page", page, "size", size,
                "totalElements", wrfDataPage.getTotalElements(),
                "totalPages", wrfDataPage.getTotalPages()
            );
        } catch (Exception e) {
            log.error("Failed to list WRF data", e);
            return Map.of("success", false, "message", "获取列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/detail")
    public Map<String, Object> getWrfDataDetail(@RequestParam("id") Long id) {
        log.info("Getting WRF data detail for id: {}", id);
        return wrfDataService.getDetail(id);
    }

    private Map<String, Object> processWrfFromFilePath(String filePath, int height, String paramType) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("filePath", filePath);
            params.put("height", height);
            if (paramType != null) {
                params.put("paramType", paramType);
            }
            String result = pythonScriptInvoker.execute(pythonScriptPath, "parse", params);
            return Map.of("success", true, "code", 200, "message", "WRF文件处理成功", "data", result);
        } catch (Exception e) {
            log.error("Failed to process WRF file from path: {}", filePath, e);
            return Map.of("success", false, "code", 500, "message", "文件处理失败");
        }
    }

    private Map<String, Object> generateMockWeatherData(int height, Map<String, Double> bounds) {
        Map<String, Object> data = new HashMap<>();
        data.put("height", height);
        data.put("timestamp", System.currentTimeMillis());
        if (bounds != null) {
            data.put("bounds", bounds);
        }
        data.put("wind_speed", generateGridData(10, 10, 5.0, 15.0));
        data.put("wind_direction", generateGridData(10, 10, 0.0, 360.0));
        data.put("temperature", generateGridData(10, 10, 15.0, 30.0));
        data.put("humidity", generateGridData(10, 10, 40.0, 80.0));
        data.put("pressure", generateGridData(10, 10, 1000.0, 1020.0));
        return Map.of("success", true, "data", data);
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
