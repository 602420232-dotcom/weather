package com.uav.common.script;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component("scriptPythonInvoker")
public class PythonScriptInvoker {

    private static final Set<String> ALLOWED_SCRIPTS = Set.of(
            "meteor_forecast.py",
            "path_planner.py",
            "assimilation.py",
            "three_layer_planner.py",
            "advanced_planners.py",
            "wrf_processor.py",
            "data_assimilation.py",
            "reinforcement_learning.py",
            "wrf/wrf_parser.py",
            "assimilation/bayesian_assimilation.py",
            "prediction/meteor_forecast.py",
            "path-planning/three_layer_planner.py",
            "vrp/optimize_routes.py"
    );

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "predict", "plan", "compute", "assimilate", "optimize",
            "vrptw", "astar", "dwa", "full",
            "global_path", "local_avoidance",
            "parse", "validate", "transform",
            "execute", "batch", "variance",
            "correct", "get_forecast", "get_detailed_forecast", "get_realtime_weather"
    );

    private static final Set<String> ALLOWED_EXECUTABLES = Set.of(
            "python3", "python", "/usr/bin/python3", "/usr/local/bin/python3",
            "C:\\Python310\\python.exe", "C:\\Python311\\python.exe", "C:\\Python312\\python.exe",
            "/usr/bin/python", "/usr/local/bin/python"
    );

    private static final int MAX_THREAD_POOL_SIZE = 10;
    private static final int THREAD_KEEP_ALIVE_SECONDS = 60;
    private static final long MAX_SCRIPT_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final long MAX_PARAMS_SIZE_BYTES = 1 * 1024 * 1024; // 1MB

    @Value("${uav.python.script-path:src/main/python}")
    private String scriptPath;

    @Value("${uav.python.timeout:30000}")
    private int timeout;

    @Value("${uav.python.enabled:true}")
    private boolean enabled;

    /**
     * ★ 2026-09-28 新增：**显式指定 Python 解释器**（留空 = 维持原行为）。
     *
     * <p>为什么需要它：{@link #getValidPythonExecutable()} 原来的判据只是
     * 「`--version` 能不能跑起来」，**不检查这个解释器有没有 numpy/netCDF4**。
     * 实测本机 PATH 里第一个命中的 `python3` 是 MSYS Python 3.14（**没有 numpy**），
     * 而真正带依赖的解释器是另一个 ⇒ 解析器会以 `ModuleNotFoundError` 失败，
     * 或者换个 PATH 顺序就悄悄换了解释器。这正是本项目反复踩的
     * 「机制写了、判据没拿真实状态核对」那一类问题。
     *
     * <p>设置后：只校验「路径存在且是普通文件」，不再走白名单扫描
     * （白名单里只有 `python3`/`python`/几个固定路径，无法覆盖 venv 场景）。
     */
    @Value("${uav.python.executable:}")
    private String pythonExecutable;

    @Value("${uav.python.security.hashing-enabled:true}")
    private boolean hashingEnabled;

    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    public PythonScriptInvoker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.executorService = new ThreadPoolExecutor(
                2,
                MAX_THREAD_POOL_SIZE,
                THREAD_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public String execute(String scriptName, String action, Map<String, Object> params) {
        return execute(scriptName, action, params, timeout);
    }

    public String execute(String scriptName, String action, Map<String, Object> params, int timeoutMs) {
        validateScriptName(scriptName);
        validateAction(action);
        validateParams(params);

        if (!enabled) {
            log.warn("Python script execution is disabled");
            return "{\"success\": false, \"error\": \"Python execution disabled\"}";
        }

        log.info("Executing Python script: {} {} (timeout={}ms)", scriptName, action, timeoutMs);

        try {
            String scriptFullPath = getSecureScriptPath(scriptName);

            if (!Files.exists(Paths.get(scriptFullPath))) {
                throw new IllegalArgumentException("Script file does not exist: " + scriptName);
            }

            validateScriptFile(scriptFullPath);

            Path tempFile = Files.createTempFile("python_params_", ".json");
            tempFile.toFile().deleteOnExit();

            try {
                byte[] paramsBytes = objectMapper.writeValueAsBytes(params);
                if (paramsBytes.length > MAX_PARAMS_SIZE_BYTES) {
                    throw new SecurityException("Parameters size exceeds maximum allowed size");
                }
                Files.write(tempFile, paramsBytes);

                ProcessBuilder pb = new ProcessBuilder(
                        getValidPythonExecutable(),
                        scriptFullPath,
                        action,
                        tempFile.toString()
                );
                // ★ 2026-09-28 修：**不再把 stderr 并进 stdout**。
                //   脚本自己写明「stdout 只留给 JSON，日志一律写 stderr」
                //   （wrf_processor.py:95-104），而这里原本是
                //   `redirectErrorStream(true)` ⇒ 日志被拼进 data。
                //   为什么一直没暴露：Java 侧用 Jackson 读时**默认容忍尾随内容**
                //   （FAIL_ON_TRAILING_TOKENS 默认关），所以 harness 判 PASS；
                //   而前端是 `JSON.parse`（严格）⇒ 直接抛
                //   `SyntaxError: Unexpected non-whitespace character after JSON`。
                //   ⇒ 这是「测试装置的解析器比前端宽松 ⇒ 判据不等价」的典型，
                //     修法是让数据通道与诊断通道分开，而不是放宽前端。
                pb.redirectErrorStream(false);
                pb.directory(new File(scriptPath));
                pb.environment().remove("PYTHONPATH");
                // 子进程的 stdio 编码钉成 UTF-8（Windows 默认是 GBK，
                // 会让 `ensure_ascii=False` 的中文/特殊符号在输出途中崩掉）。
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.environment().put("PYTHONUTF8", "1");

                Process process = pb.start();

                // stderr（诊断通道）单独抽干：不并流，但也不能不读 —— 否则
                // 缓冲区写满会把子进程卡死。
                Future<String> errFuture = executorService.submit(
                        () -> readStream(process.getErrorStream()));
                Future<String> future = executorService.submit(
                        () -> readStream(process.getInputStream()));

                try {
                    String result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                    String errText = awaitQuietly(errFuture, 3000);
                    int exitValue = process.waitFor();

                    log.info("Python script completed: {} {} (exit={}, stdout={}B, stderr={}B)",
                            scriptName, action, exitValue, result.length(),
                            errText == null ? 0 : errText.length());
                    if (errText != null && !errText.isBlank()) {
                        log.info("Python stderr: {}", tail(errText, 4000));
                    }

                    if (exitValue != 0) {
                        // ★ 2026-09-28 修：原判据是 `exitValue != 0 && result.isEmpty()`，
                        //   意思是「只要吐了东西就算成功」。实测后果很坏：脚本在输出
                        //   1.1 MB 之后因编码崩溃，stdout 里留下半段 JSON，
                        //   而这个判据把它**当成正常结果**返回；上层
                        //   `WrfController` 再包一层 `success=true` ⇒ 接口看起来是通的。
                        //   （正是 README 第 68 条那个陷阱：不报错、有数出、看着像真的。）
                        //   现在：非零退出码一律返回 success=false 的 JSON，
                        //   并把 stdout/stderr 尾部作为 output 一起带回去便于定位。
                        log.error("Python script failed: {} {} (exit={})", scriptName, action, exitValue);
                        return errorJson("Python 脚本以非零退出码结束: " + exitValue,
                                (errText == null ? "" : errText) + result);
                    }

                    return result;

                } catch (TimeoutException e) {
                    process.destroyForcibly();
                    future.cancel(true);
                    errFuture.cancel(true);
                    log.error("Python script timeout: {} {}", scriptName, action);
                    return "{\"success\": false, \"error\": \"Execution timeout after " + timeoutMs + "ms\"}";
                }

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (SecurityException e) {
            log.error("Security validation failed: {}", e.getMessage());
            return "{\"success\": false, \"error\": \"Security validation failed: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            log.error("Python script execution failed: {} {}", scriptName, action, e);
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public CompletableFuture<String> executeAsync(String scriptName, String action, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> execute(scriptName, action, params), executorService);
    }

    public String executeWithRetry(String scriptName, String action, Map<String, Object> params, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxRetries) {
            try {
                String result = execute(scriptName, action, params);
                if (result != null && !result.contains("\"success\": false")) {
                    return result;
                }
            } catch (Exception e) {
                lastException = e;
            }

            attempts++;
            if (attempts < maxRetries) {
                log.warn("Retry {}/{} for script {} {}", attempts, maxRetries, scriptName, action);
                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("All retries exhausted for script {} {}", scriptName, action, lastException);
        return "{\"success\": false, \"error\": \"All retries exhausted: " + (lastException != null ? lastException.getMessage() : "unknown") + "\"}";
    }

    private void validateScriptName(String scriptName) {
        if (scriptName == null || scriptName.trim().isEmpty()) {
            throw new SecurityException("Script name cannot be null or empty");
        }

        if (scriptName.contains("..") || scriptName.contains("\\")) {
            throw new SecurityException("Invalid script name: path traversal detected");
        }

        if (!ALLOWED_SCRIPTS.contains(scriptName)) {
            throw new SecurityException("Script not in allowed list: " + scriptName);
        }
    }

    private void validateAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new SecurityException("Action cannot be null or empty");
        }

        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new SecurityException("Action not in allowed list: " + action);
        }
    }

    private String getSecureScriptPath(String scriptName) {
        String basePath = scriptPath.isEmpty() ? "src/main/python" : scriptPath;
        Path fullPath = Paths.get(basePath, scriptName).normalize();
        Path baseDir = Paths.get(basePath).normalize();

        if (!fullPath.startsWith(baseDir)) {
            throw new SecurityException("Path traversal attempt detected");
        }

        // ★ 2026-09-28 修：**返回绝对路径**。
        //   原实现返回相对路径（`src/main/python/wrf_processor.py`），而调用处
        //   同时还设了 `pb.directory(new File(scriptPath))` ⇒ 相对命令路径会被
        //   **再拼一次工作目录**，实测报错：
        //     python.exe: can't open file
        //       '…\wrf-processor\src\main\python\src\main\python\wrf_processor.py'
        //     → 退出码 2（而且 stderr 才说得清，stdout 是空的）。
        //   `uav.python.script-path` 的默认值就是相对的 `src/main/python`
        //   ⇒ 任何"没显式改成绝对路径"的部署都会踩到（配置看起来完全正常）。
        //   绝对化后，命令路径与工作目录解耦，这个坑消失。
        return fullPath.toAbsolutePath().normalize().toString();
    }

    public boolean isPythonAvailable() {
        try {
            Process process = new ProcessBuilder("python3", "--version")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("Python is not available", e);
            return false;
        }
    }

    public Set<String> getAllowedScripts() {
        return ALLOWED_SCRIPTS;
    }

    public Set<String> getAllowedActions() {
        return ALLOWED_ACTIONS;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Map<String, Object> executeAsMap(String scriptName, String action, Map<String, Object> params) {
        String result = execute(scriptName, action, params);
        try {
            return objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse Python result as Map, returning raw: {}", e.getMessage());
            return Map.of("success", false, "error", "Failed to parse result", "raw", result);
        }
    }

    private void validateParams(Map<String, Object> params) {
        if (params == null) {
            throw new SecurityException("Parameters cannot be null");
        }
        validateParamsRecursive(params, "", 0);
    }

    private void validateParamsRecursive(Object value, String path, int depth) {
        if (depth > 10) {
            throw new SecurityException("Parameter nesting depth exceeds maximum allowed");
        }

        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            for (Object key : map.keySet()) {
                if (!(key instanceof String)) {
                    throw new SecurityException("Parameter keys must be strings");
                }
                String keyStr = (String) key;
                if (keyStr.contains("..") || keyStr.contains("/") || keyStr.contains("\\")) {
                    throw new SecurityException("Invalid parameter key: " + keyStr);
                }
                validateParamsRecursive(map.get(key), path + "." + keyStr, depth + 1);
            }
        } else if (value instanceof Iterable) {
            int index = 0;
            for (Object item : (Iterable<?>) value) {
                validateParamsRecursive(item, path + "[" + index + "]", depth + 1);
                index++;
            }
        } else if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.length() > 100000) {
                throw new SecurityException("String value too long at " + path);
            }
        }
    }

    /**
     * 抽干一个子进程流并按 **UTF-8** 解码（★ 2026-09-26/28 两处修复的落点）。
     *
     * <p>原实现用的是平台默认字符集（Windows = GBK），而子进程按 UTF-8 输出
     * （脚本里有中文 warnings/notes 且 `ensure_ascii=False`）⇒ 中文变乱码，
     * 但 JSON 结构侥幸还在 ⇒ **不报错、只静默损坏数据**。
     */
    private String readStream(java.io.InputStream in) throws java.io.IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (output.length() > MAX_SCRIPT_SIZE_BYTES) {
                    log.warn("Script stream exceeded maximum size, truncating");
                    break;
                }
            }
        }
        return output.toString();
    }

    /** 取一个已完成 Future 的值；超时/异常都不影响主流程（诊断信息而已）。 */
    private String awaitQuietly(Future<String> future, long millis) {
        try {
            return future.get(millis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("读取子进程 stderr 失败/超时: {}", e.getMessage());
            return null;
        }
    }

    private static String tail(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(s.length() - max);
    }

    /**
     * 构造「失败」响应（JSON 字符串）。
     *
     * <p>用 ObjectMapper 而不是手工拼字符串：Traceback 里有引号、换行、中文，
     * 拼接会让 JSON 结构本身破掉 —— 而调用方（前端 `unwrapParserPayload`）
     * 恰恰要 `JSON.parse` 它。
     */
    private String errorJson(String message, String outputTail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("error", message);
        if (outputTail != null && !outputTail.isEmpty()) {
            payload.put("output", outputTail.length() > 2000
                    ? outputTail.substring(outputTail.length() - 2000) : outputTail);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("序列化失败响应时出错: {}", e.getMessage());
            return "{\"success\": false, \"error\": \"Python script execution failed\"}";
        }
    }

    private void validateScriptFile(String scriptPath) throws Exception {
        Path path = Paths.get(scriptPath);
        
        if (!Files.isRegularFile(path)) {
            throw new SecurityException("Path is not a regular file: " + scriptPath);
        }

        long fileSize = Files.size(path);
        if (fileSize > MAX_SCRIPT_SIZE_BYTES) {
            throw new SecurityException("Script file exceeds maximum allowed size");
        }

        if (Files.isSymbolicLink(path)) {
            throw new SecurityException("Symbolic links are not allowed");
        }
    }

    private String getValidPythonExecutable() {
        // ★ 2026-09-28：显式配置优先（见 pythonExecutable 字段注释）。
        if (pythonExecutable != null && !pythonExecutable.isBlank()) {
            Path configured = Paths.get(pythonExecutable).toAbsolutePath().normalize();
            if (!Files.isRegularFile(configured)) {
                throw new SecurityException(
                        "配置的 Python 解释器不存在或不是普通文件: " + configured);
            }
            log.info("Using configured Python interpreter: {}", configured);
            return configured.toString();
        }
        for (String executable : ALLOWED_EXECUTABLES) {
            try {
                Process process = new ProcessBuilder(executable, "--version")
                        .redirectErrorStream(true)
                        .start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    log.info("Using auto-detected Python interpreter: {} "
                            + "(未配置 uav.python.executable：仅探测了能否启动，"
                            + "未校验 numpy/netCDF4 依赖是否齐备)", executable);
                    return executable;
                }
            } catch (Exception e) {
                log.debug("Python executable not available: {}", executable, e);
            }
        }
        throw new SecurityException("No valid Python executable found in allowed list");
    }

}
