import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.uav.common.script.PythonScriptInvoker;
import com.uav.wrf.processor.controller.WrfController;
import com.uav.wrf.processor.entity.WrfDataFile;
import com.uav.wrf.processor.repository.WrfDataFileRepository;
import com.uav.wrf.processor.service.WrfChunkUploadService;
import com.uav.wrf.processor.service.WrfDataService;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 把**生产代码里的 WrfController** 架在一个 JDK 自带的 HttpServer 上，
 * 供浏览器端到端验证使用。
 *
 * <p>★ 2026-09-28：本文件从 `services/wrf-processor/target/_verify/` 移到 `tools/verify/`
 * —— 原位置在 `.gitignore:55`（`target/`）里，一次 `mvn clean` 就全丢。
 *
 * <p>真实的东西：WrfController / WrfDataService / WrfChunkUploadService /
 *            PythonScriptInvoker / wrf_processor.py / 真实 wrfout / 真实 HTTP。
 * 合成的东西：Servlet 容器（用 JDK HttpServer 代替 Tomcat）+
 *            一个只回答 findByFileId/save 的仓库桩。
 *
 * <p>⚠️ 它**不是**「真实 Spring 上下文」：没有 Spring MVC 的 multipart 绑定、
 * 没有 `JwtSecurityConfig` 过滤链、仓库是动态代理桩。
 * 「真实上下文」的判据由 `WrfPlatformIntegrationTest`（`@SpringBootTest`）承担。
 *
 * 之所以监听 8089：那正是 frontend/vite.config.js 里 `/api` 的 proxy target
 * ⇒ **不需要改任何前端配置**，浏览器访问 vite dev server 的 /api 就会打到这儿。
 */
public final class PlatformHttpServer {

    static final String FILE_ID = "harness_file";
    static ObjectMapper M = new ObjectMapper();
    static StringBuilder LOG = new StringBuilder();
    static Path LOG_FILE;

    public static void main(String[] args) throws Exception {
        Path wrfout = Paths.get(args[0]).toAbsolutePath();
        Path dataDir = Paths.get(args[1]).toAbsolutePath();
        Path pyDir = Paths.get(args[2]).toAbsolutePath();
        int port = Integer.parseInt(args[3]);
        LOG_FILE = Paths.get(args[4]).toAbsolutePath();
        Files.deleteIfExists(LOG_FILE);
        Files.createDirectories(dataDir);

        WrfDataFile rec = WrfDataFile.builder()
                .id(1L).fileId(FILE_ID).fileName(wrfout.getFileName().toString())
                .filePath(wrfout.toString()).fileSize(Files.size(wrfout)).height(100)
                .timeSteps(1).variables("T,U,V,W,RH,WS").status("UPLOADED").build();
        List<WrfDataFile> store = new ArrayList<>();
        store.add(rec);

        WrfDataFileRepository repo = (WrfDataFileRepository) Proxy.newProxyInstance(
                PlatformHttpServer.class.getClassLoader(),
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
        // ★ 2026-09-28：显式解释器（第 6 个参数，可选）。
        //   不传就退回自动探测 —— 而自动探测只看 `--version`，不查 numpy/netCDF4，
        //   本机 PATH 里第一个 `python3`（MSYS 3.14）恰好没有依赖 ⇒ 解析必失败。
        if (args.length > 5 && !args[5].isBlank()) {
            set(invoker, "pythonExecutable", args[5]);
        }

        WrfDataService service = new WrfDataService(repo, invoker);
        set(service, "pythonScriptPath", "wrf_processor.py");

        WrfChunkUploadService chunkService = new WrfChunkUploadService();
        set(chunkService, "dataPath", dataDir.toString());

        WrfController controller = new WrfController(service, invoker, chunkService);
        set(controller, "pythonScriptPath", "wrf_processor.py");
        set(controller, "dataPath", dataDir.toString());

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        server.createContext("/api/wrf/parse", ex -> {
            try {
                Map<String, String> q = query(ex);
                int height = Integer.parseInt(q.getOrDefault("height", "100"));
                Map<String, byte[]> parts = multipart(ex);
                Map<String, byte[]> f = firstFile(parts);
                MockMultipartFile mf = new MockMultipartFile("file", f.keySet().iterator().next(),
                        "application/octet-stream", f.values().iterator().next());
                json(ex, controller.parseWrfFile(mf, height));
            } catch (Exception e) {
                fail(ex, e);
            }
        });

        server.createContext("/api/wrf/upload/chunk", ex -> {
            try {
                if ("DELETE".equals(ex.getRequestMethod())) {
                    Map<String, String> q = query(ex);
                    json(ex, controller.cancelUpload(q.get("fileKey")));
                    return;
                }
                Map<String, String> q = query(ex);
                Map<String, byte[]> parts = multipart(ex);
                Map<String, byte[]> f = firstFile(parts);
                MockMultipartFile mf = new MockMultipartFile("file", f.keySet().iterator().next(),
                        "application/octet-stream", f.values().iterator().next());
                // ★ 2026-09-28 修（这正是上一轮浏览器 E2E 拿到 500 的根因）：
                //   原实现从**查询串** q 取 chunkIndex/totalChunks/fileKey/fileName，
                //   而前端（NetCDFChunkUploader.vue:656-664）是把它们放进 **FormData** 的
                //   ⇒ q.get(...) 恒为 null ⇒ Integer.parseInt(null) ⇒
                //   NumberFormatException ⇒ HTTP 500（见 target/_verify/http_server.log:4-6）。
                //   现在：表单字段优先，查询串兜底（Spring 的 @RequestParam 本来就是两者都收）。
                json(ex, controller.uploadChunk(mf,
                        Integer.parseInt(pick(parts, "chunkIndex", q.get("chunkIndex"))),
                        Integer.parseInt(pick(parts, "totalChunks", q.get("totalChunks"))),
                        pick(parts, "fileKey", q.get("fileKey")),
                        pick(parts, "fileName", q.get("fileName")),
                        pick(parts, "chunkHash", q.get("chunkHash"))));
            } catch (Exception e) {
                fail(ex, e);
            }
        });

        server.createContext("/api/wrf/upload/status", ex -> {
            try {
                Map<String, String> q = query(ex);
                Map<String, byte[]> parts = "POST".equals(ex.getRequestMethod()) ? multipart(ex) : Map.of();
                String tc = pick(parts, "totalChunks", q.get("totalChunks"));
                json(ex, controller.uploadStatus(pick(parts, "fileKey", q.get("fileKey")),
                        tc == null ? null : Integer.valueOf(tc)));
            } catch (Exception e) {
                fail(ex, e);
            }
        });

        server.createContext("/api/wrf/upload/merge", ex -> {
            try {
                json(ex, controller.mergeChunks(readJson(ex)));
            } catch (Exception e) {
                fail(ex, e);
            }
        });

        server.createContext("/api/wrf/parse-uploaded", ex -> {
            try {
                json(ex, controller.parseUploaded(readJson(ex)));
            } catch (Exception e) {
                fail(ex, e);
            }
        });

        server.createContext("/api/wrf/turbulence", ex -> {
            try {
                json(ex, controller.getTurbulence(query(ex).get("fileId")));
            } catch (Exception e) {
                fail(ex, e);
            }
        });
        server.createContext("/api/wrf/visibility", ex -> {
            try {
                json(ex, controller.getVisibility(query(ex).get("fileId")));
            } catch (Exception e) {
                fail(ex, e);
            }
        });
        server.createContext("/api/wrf/lightning-risk", ex -> {
            try {
                json(ex, controller.getLightningRisk(query(ex).get("fileId")));
            } catch (Exception e) {
                fail(ex, e);
            }
        });
        server.createContext("/api/wrf/health", ex -> {
            try {
                json(ex, Map.of("status", "UP", "handler", "PlatformHttpServer"));
            } catch (Exception e) {
                fail(ex, e);
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
        log("PlatformHttpServer listening on 127.0.0.1:" + port
                + "  (wrfout=" + wrfout.getFileName() + ", dataDir=" + dataDir + ")");
        Thread.currentThread().join();
    }

    // ────────────────────────────────────────────────── HTTP 小工具

    static void json(HttpExchange ex, Object body) throws Exception {
        byte[] bytes = M.writeValueAsBytes(body);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        log(ex.getRequestMethod() + " " + ex.getRequestURI()
                + "  → 200 (" + bytes.length + " B)");
    }

    static void fail(HttpExchange ex, Exception e) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            byte[] bytes = M.writeValueAsBytes(m);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
            log(ex.getRequestMethod() + " " + ex.getRequestURI() + "  → 500 " + e);
        } catch (Exception inner) {
            log("!! fail() 自身出错: " + inner);
        }
    }

    static Map<String, String> query(HttpExchange ex) {
        Map<String, String> out = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) {
            return out;
        }
        for (String kv : raw.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) {
                out.put(urlDecode(kv.substring(0, i)), urlDecode(kv.substring(i + 1)));
            }
        }
        return out;
    }

    static String urlDecode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static Map<String, Object> readJson(HttpExchange ex) throws Exception {
        byte[] b = readAll(ex.getRequestBody());
        if (b.length == 0) {
            return new HashMap<>();
        }
        return M.readValue(b, Map.class);
    }

    static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /**
     * 极简 multipart 解析（harness 胶水，不是产品代码）。
     *
     * 做法：把整包按 ISO-8859-1 转成 String（1 字节 ↔ 1 字符，无损），
     * 用 boundary 切段后各段再转回 byte[]。这样二进制分片不会被字符解码破坏。
     */
    static Map<String, byte[]> multipart(HttpExchange ex) throws Exception {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.contains("boundary=")) {
            throw new IllegalArgumentException("不是 multipart 请求: " + ct);
        }
        String boundary = ct.substring(ct.indexOf("boundary=") + 9).trim();
        if (boundary.startsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        byte[] body = readAll(ex.getRequestBody());
        String s = new String(body, StandardCharsets.ISO_8859_1);
        String delim = "--" + boundary;

        Map<String, byte[]> out = new LinkedHashMap<>();
        int pos = s.indexOf(delim);
        if (pos < 0) {
            throw new IllegalArgumentException("找不到 boundary");
        }
        while (true) {
            int next = s.indexOf(delim, pos + delim.length());
            if (next < 0) {
                break;
            }
            String seg = s.substring(pos + delim.length(), next);
            int he = seg.indexOf("\r\n\r\n");
            if (he >= 0) {
                String headers = seg.substring(0, he);
                String content = seg.substring(he + 4);
                if (content.endsWith("\r\n")) {
                    content = content.substring(0, content.length() - 2);
                }
                String name = attr(headers, "name");
                String filename = attr(headers, "filename");
                if (name != null) {
                    out.put(filename != null ? name + "|" + filename : name,
                            content.getBytes(StandardCharsets.ISO_8859_1));
                }
            }
            pos = next;
        }
        return out;
    }

    static String attr(String headers, String key) {
        int i = headers.indexOf(key + "=\"");
        if (i < 0) {
            return null;
        }
        int start = i + key.length() + 2;
        int end = headers.indexOf('"', start);
        return end < 0 ? null : headers.substring(start, end);
    }

    /** 取第一个带 filename 的 part。 */
    static Map<String, byte[]> firstFile(Map<String, byte[]> parts) {
        for (Map.Entry<String, byte[]> e : parts.entrySet()) {
            if (e.getKey().contains("|")) {
                String fn = e.getKey().substring(e.getKey().indexOf('|') + 1);
                if (fn != null && !fn.isBlank()) {
                    Map<String, byte[]> one = new LinkedHashMap<>();
                    one.put(fn, e.getValue());
                    return one;
                }
            }
        }
        throw new IllegalArgumentException("multipart 里没有文件 part: " + parts.keySet());
    }

    /**
     * ★ 2026-09-28 新增：取 multipart 里的**文本字段**（非文件 part）。
     *
     * <p>{@link #multipart} 对文本 part 用的键就是 `name`，对文件 part 用 `name|filename`。
     * 返回 null 表示该字段不在表单里（调用方再退到查询串）。
     */
    static String text(Map<String, byte[]> parts, String name) {
        byte[] v = parts.get(name);
        if (v == null) {
            return null;
        }
        String s = new String(v, StandardCharsets.UTF_8).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 表单字段优先、查询串兜底 —— 与 Spring {@code @RequestParam} 对 multipart 的
     * 实际语义一致（两者都收）。这一行就是 2026-09-26 那次 500 的修复点。
     */
    static String pick(Map<String, byte[]> parts, String name, String fromQuery) {
        String fromForm = text(parts, name);
        return fromForm != null ? fromForm : fromQuery;
    }

    static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    static synchronized void log(String line) {
        String s = java.time.LocalTime.now() + "  " + line;
        LOG.append(s).append(System.lineSeparator());
        System.out.println(s);
        try {
            Files.writeString(LOG_FILE, s + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // 日志失败不影响服务
        }
    }
}
