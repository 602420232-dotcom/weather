package com.uav.wrf.processor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ★ C1/C2/C3 的**真实 Spring 上下文**端到端集成测试（2026-09-28 新增）。
 *
 * <p>与 `tools/verify/` 下那套 harness 的区别（那套保留，用于快速调试）：
 * <ul>
 *   <li>这里是**真的 Spring Boot**：真 Undertow、真的
 *       {@code JwtSecurityConfig} 过滤链（{@code common/.../security/JwtSecurityConfig.java}）、
 *       真的 Spring MVC multipart 绑定、真 JPA(H2)、真的
 *       {@code PythonScriptInvoker} → 子进程 {@code wrf_processor.py}。</li>
 *   <li>harness 那套用 JDK HttpServer + 动态代理仓库桩，**绕过**了过滤链与 multipart 绑定
 *       ⇒ 它证明不了"经过真实 Spring 还通"。</li>
 * </ul>
 *
 * <p>★ 判据设计原则（本项目的教训）：**只写 mock 不可能通过的东西**，
 * 且**用与浏览器等价的方式解析**。
 * <ul>
 *   <li>JSON 解析启用 {@code FAIL_ON_TRAILING_TOKENS}：浏览器是
 *       {@code JSON.parse}（严格），而 Jackson 默认**容忍尾随内容** ——
 *       上一轮 harness 就是这样把"stderr 混进 stdout"的缺陷判成了 PASS。</li>
 *   <li>"纬度/经度"不是写死的 129/149，而是**用 netCDF4 独立读一遍样例文件**
 *       再比对（这样样例换成裁切版也依然有效，且不拿实现给自己作证）。</li>
 *   <li>C2 用**两份输入**（原文件 + 扰动版）证明输出随输入变，且端点数值
 *       必须**逐个等于同一次解析的 {@code risk} 块**（不是等于某个常量）。</li>
 * </ul>
 *
 * <p>样例 wrfout 的解析顺序：{@code -Dwrf.test.nc=…} → 环境变量 {@code WRF_TEST_NC}
 * → {@code src/test/resources/verify/sample_wrfout.nc}；都没有则**跳过并说明原因**
 * （不静默通过）。生成方式见 {@code tools/verify/README.md}。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // ★ 关键：src/test/resources/application.yml 为了跑单测把
                //   SecurityAutoConfiguration 排除了；不解除排除，JwtSecurityConfig
                //   这个 SecurityFilterChain 就是**死配置**（没有 WebSecurityEnabler
                //   去装配 filter chain）⇒ 测的就不是生产栈。
                "spring.autoconfigure.exclude=",
                // ★ 生产 application.yml 里就是 true（Feign 客户端同名 Bean 会重复注册）；
                //   而 src/test/resources/application.yml **整份替换**了它 ⇒ 必须在这里补上，
                //   否则上下文起不来（BeanDefinitionOverrideException:
                //   buoy-weather-service.FeignClientSpecification）。这是"复刻生产配置"，
                //   不是放宽判据。
                "spring.main.allow-bean-definition-overriding=true",
                // ★ 2026-09-28 实测暴露：本服务用 Undertow，而**没有任何 multipart 上限配置**
                //   ⇒ Undertow 默认「单个文件 1 MB」生效，前端整文件 POST（设计上限 2 GB）
                //   一律 500（`UT000054: The maximum size 1048576 ... exceeded`）。
                //   这里复刻 production application.yml 里新增的同名配置。
                "spring.servlet.multipart.max-file-size=2048MB",
                "spring.servlet.multipart.max-request-size=2048MB",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "wrf.python-script=wrf_processor.py",
                "uav.python.enabled=true",
                "uav.python.timeout=300000",
                "uav.jwt.enabled=true",
                // HS512 需要 ≥64 字节密钥；测试专用，非生产密钥。
                "uav.jwt.secret=it-only-secret-0123456789abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789"
        })
class WrfPlatformIntegrationTest {

    private static final int CHUNK_SIZE = 5 * 1024 * 1024;   // 与前端 CHUNK_SIZE 一致
    private static final String JWT_SECRET =
            "it-only-secret-0123456789abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789";
    private static final List<String> REQUIRED_VARS = List.of("T", "U", "V", "W", "RH", "WS");

    @Autowired
    private TestRestTemplate rest;

    // ── 运行时解析出的外部依赖（Python 解释器 / 脚本目录 / 数据目录 / 样例文件）──

    private static Path python;
    private static Path scriptDir;
    private static Path dataDir;
    private static Path fixture;

    @DynamicPropertySource
    static void wirePaths(DynamicPropertyRegistry registry) {
        python = firstUsablePython();
        scriptDir = Paths.get("src", "main", "python").toAbsolutePath().normalize();
        dataDir = Paths.get("target", "it-data").toAbsolutePath().normalize();
        fixture = locateFixture();
        registry.add("uav.python.executable", () -> python == null ? "" : python.toString());
        registry.add("uav.python.script-path", scriptDir::toString);
        registry.add("wrf.data-path", dataDir::toString);
    }

    /** 候选顺序：显式参数 → WRF_TEST_PYTHON/WRF_PYTHON → vrp-env → Anaconda；必须能 import numpy+netCDF4。 */
    private static Path firstUsablePython() {
        return resolveUsablePython();
    }

    /** 供本类与 {@code C1BrowserE2ETest} 共用：解析一个真的装了 numpy+netCDF4 的解释器。 */
    static Path resolveUsablePython() {
        List<String> candidates = new ArrayList<>();
        if (System.getProperty("wrf.test.python") != null) {
            candidates.add(System.getProperty("wrf.test.python"));
        }
        for (String env : List.of("WRF_TEST_PYTHON", "WRF_PYTHON")) {
            if (System.getenv(env) != null) {
                candidates.add(System.getenv(env));
            }
        }
        candidates.add("D:\\Developer\\workplace\\vrp-env\\Scripts\\python.exe");
        candidates.add("D:\\Developer\\Anaconda\\python.exe");
        for (String c : candidates) {
            Path p = Paths.get(c);
            if (Files.isRegularFile(p) && canImportNumpyAndNetcdf(p)) {
                return p;
            }
        }
        return null;
    }

    private static boolean canImportNumpyAndNetcdf(Path exe) {
        try {
            Process pr = new ProcessBuilder(exe.toString(), "-c", "import numpy, netCDF4")
                    .redirectErrorStream(true).start();
            pr.getInputStream().readAllBytes();
            return pr.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Path locateFixture() {
        String prop = System.getProperty("wrf.test.nc");
        if (prop == null) {
            prop = System.getenv("WRF_TEST_NC");
        }
        if (prop != null && Files.isRegularFile(Paths.get(prop))) {
            return Paths.get(prop).toAbsolutePath().normalize();
        }
        Path sample = Paths.get("src", "test", "resources", "verify", "sample_wrfout.nc")
                .toAbsolutePath().normalize();
        return Files.isRegularFile(sample) ? sample : null;
    }

    private void requireEnvironment() {
        Assumptions.assumeTrue(python != null,
                "找不到带 numpy+netCDF4 的 Python ⇒ 无法做真实解析链路验证。"
                        + "设置 -Dwrf.test.python=<path> 或环境变量 WRF_TEST_PYTHON。");
        Assumptions.assumeTrue(fixture != null,
                "找不到样例 wrfout ⇒ 跳过（**不算通过**）。"
                        + "设置 -Dwrf.test.nc=<path> / 环境变量 WRF_TEST_NC，"
                        + "或按 tools/verify/README.md 生成 src/test/resources/verify/sample_wrfout.nc。");
    }

    // ══════════════════════════════════════════════════════════════════
    //  0. 先证明「这确实是生产安全栈」——否则后面测的不是同一条链路
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("安全栈真的生效：不带 JWT 调 /api/wrf/** 必须 401")
    void securityChainIsActive() {
        ResponseEntity<String> noToken = rest.getForEntity(
                "/api/wrf/turbulence?fileId=whatever", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, noToken.getStatusCode(),
                "不带 Authorization 竟然不是 401 ⇒ JwtSecurityConfig 没生效，"
                        + "这个上下文不是生产栈（那后面所有结论都不能算数）。实际: "
                        + noToken.getStatusCode() + " " + trim(noToken.getBody()));

        ResponseEntity<String> badToken = rest.exchange(
                "/api/wrf/turbulence?fileId=whatever", HttpMethod.GET,
                new HttpEntity<>(headers("not-a-jwt")), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, badToken.getStatusCode(),
                "伪造 token 竟然不是 401。实际: " + badToken.getStatusCode());
    }

    // ══════════════════════════════════════════════════════════════════
    //  C1 + C2 + C3：一条真实链路走到底
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("C1/C2/C3 真实 Spring 上下文端到端（真 JWT + 真 multipart + 真 wrfout）")
    void c1c2c3OverRealSpringContext() throws Exception {
        requireEnvironment();
        Files.createDirectories(dataDir);

        String token = jwt("it-user");
        byte[] raw = Files.readAllBytes(fixture);
        String sha = sha256Hex(raw);
        System.out.println("[IT] fixture = " + fixture);
        System.out.println("[IT] bytes=" + raw.length + " sha256=" + sha);
        System.out.println("[IT] python  = " + python);

        // ── C1：整文件 multipart /parse（前端 parseViaBackend 的目标端点）──
        JsonNode parsed = parseWholeFile(token, raw, fixture.getFileName().toString());
        assertC1Payload(parsed, "C1/parse");
        String fileIdFromParse = parsed.path("_fileId").asText(null);
        assertNotNull(fileIdFromParse, "/parse 必须返回 fileId");

        // ── C2：三个诊断端点必须等于**同一次解析**的 risk 块（不是常量）──
        JsonNode risk = json(parsed.path("data").asText()).path("risk");
        assertTrue(risk.has("turbulence"), "解析器必须给出 risk.turbulence");
        assertC2Endpoints(token, fileIdFromParse, risk, "A");

        // ── C2 的反面：换一份**输入**，输出必须跟着变（否则就是常量）──
        Path perturbed = perturbedFixture();
        JsonNode parsedB = parseWholeFile(token, Files.readAllBytes(perturbed), perturbed.getFileName().toString());
        String fileIdB = parsedB.path("_fileId").asText();
        assertC2Differs(token, fileIdFromParse, fileIdB);

        // ── C3：分片上传（字段在 multipart 表单里，与前端 FormData 等价）──
        String fileKey = "it-" + System.currentTimeMillis();
        int total = (raw.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        Map<Integer, String> hashes = new LinkedHashMap<>();
        for (int i = 0; i < total; i++) {
            int from = i * CHUNK_SIZE;
            int to = Math.min(from + CHUNK_SIZE, raw.length);
            byte[] part = java.util.Arrays.copyOfRange(raw, from, to);
            String chunkHash = sha256Hex(part);
            hashes.put(i, chunkHash);
            JsonNode r = chunk(token, fileKey, fixture.getFileName().toString(), i, total, chunkHash, part);
            assertTrue(r.path("success").asBoolean(), "分片 " + i + " 上传失败: " + r);
        }
        JsonNode status = get(token, "/api/wrf/upload/status?fileKey=" + fileKey + "&totalChunks=" + total);
        assertTrue(status.path("complete").asBoolean(), "status 应报告 complete=true: " + status);

        // C3 负向判据：故意报错一个分片哈希 ⇒ merge 必须拒绝（不能悄悄合出一个坏文件）
        Map<Integer, String> badHashes = new LinkedHashMap<>(hashes);
        badHashes.put(0, sha256Hex("tampered".getBytes(StandardCharsets.UTF_8)));
        JsonNode badMerge = postJson(token, "/api/wrf/upload/merge", Map.of(
                "fileKey", fileKey, "fileName", fixture.getFileName().toString(),
                "totalChunks", total, "chunkHashes", badHashes));
        assertFalse(badMerge.path("success").asBoolean(),
                "分片哈希不匹配时 merge 竟然成功了 —— 断点续传的完整性校验形同虚设: " + badMerge);

        JsonNode merge = postJson(token, "/api/wrf/upload/merge", Map.of(
                "fileKey", fileKey, "fileName", fixture.getFileName().toString(),
                "totalChunks", total, "chunkHashes", hashes));
        assertTrue(merge.path("success").asBoolean(), "merge 失败: " + merge);
        assertEquals(sha, merge.path("data").path("sha256").asText(),
                "合并产物 sha256 必须与原始文件逐位一致");
        String mergedFileId = merge.path("data").path("fileId").asText();

        JsonNode viaFileId = postJson(token, "/api/wrf/parse-uploaded", Map.of("fileId", mergedFileId));
        assertTrue(viaFileId.path("success").asBoolean(), "parse-uploaded 失败: " + viaFileId);
        assertC1Payload(viaFileId, "C3/parse-uploaded");

        // ★ 回归判据：**经 /parse 上传拿到的 fileId 也必须能继续用**
        //   （2026-09-28 修的缺陷：原来登记完 fileId 就把文件删了 ⇒
        //    /turbulence 等按 fileId 取数的端点对这个 fileId 全部失效）
        assertC2Endpoints(token, fileIdFromParse, risk, "A(fileId 来自 /parse)");
    }

    // ── C1 判据 ─────────────────────────────────────────────────────

    private void assertC1Payload(JsonNode body, String tag) throws Exception {
        assertTrue(body.path("success").asBoolean(), tag + " 应 success=true: " + trim(body.toString()));
        String data = body.path("data").asText();
        // ★ 严格解析：浏览器是 JSON.parse，Jackson 默认会容忍尾随内容。
        JsonNode parsed = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()
                .readTree(data);
        assertTrue(parsed.path("success").asBoolean(), tag + " 解析器内部 success 必须为 true");
        JsonNode payload = parsed.path("payload");

        // 与样例文件**独立读取**的真实维度比对（不写死 129/149）
        int[] dims = readNetcdfDims(fixture);
        assertEquals(dims[0], payload.path("dims").path("lat").asInt(),
                tag + " 纬度维必须等于样例文件真实维度（mock 给 30）");
        assertEquals(dims[1], payload.path("dims").path("lon").asInt(),
                tag + " 经度维必须等于样例文件真实维度（mock 给 40）");
        assertEquals(1, payload.path("dims").path("time").asInt(), tag + " 时间维应为 1");

        // height=100 ⇒ 解析器只返回请求的那一层（FIX-HEIGHT）；mock 给 10 层 hPa
        assertEquals(1, payload.path("dims").path("level").asInt(),
                tag + " height=100 时只应返回 1 层（mock 给 10）");
        assertTrue(payload.path("levelNames").get(0).asText().contains("100m AGL"),
                tag + " levelNames[0] 应是 '100m AGL'（mock 给 hPa）: "
                        + payload.path("levelNames").get(0).asText());

        List<String> vars = new ArrayList<>();
        payload.path("vars").forEach(v -> vars.add(v.asText()));
        assertTrue(vars.containsAll(REQUIRED_VARS), tag + " 变量应含 " + REQUIRED_VARS + "，实际 " + vars);
        assertFalse(vars.contains("T2"), tag + " 不应出现 mock 的 T2 变量名，实际 " + vars);

        // 温度必须是真实 K 值域（mock 是 15~30 的"摄氏度"随机数）
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (JsonNode row : payload.path("temperature").get(0)) {
            for (JsonNode cell : row) {
                min = Math.min(min, cell.asDouble());
                max = Math.max(max, cell.asDouble());
            }
        }
        assertTrue(min > 200 && max < 330,
                tag + " 第一层温度应在真实 K 值域内（mock 给 15~30），实际 min=" + min + " max=" + max);
        System.out.println("[IT] " + tag + " OK: dims=" + payload.path("dims") + " vars=" + vars
                + " T∈[" + String.format("%.2f", min) + "," + String.format("%.2f", max) + "]");
    }

    // ── C2 判据 ─────────────────────────────────────────────────────

    /** 三个端点的数值必须**逐个等于同一次解析的 risk 块** —— 这同时排除了"常量"和"另算一套"。 */
    private void assertC2Endpoints(String token, String fileId, JsonNode risk, String tag) {
        JsonNode turb = get(token, "/api/wrf/turbulence?fileId=" + fileId);
        assertTrue(turb.path("success").asBoolean(), tag + " /turbulence 失败: " + trim(turb.toString()));
        assertClose(risk.path("turbulence").path("tke_max").asDouble(),
                turb.path("data").path("tke_max").asDouble(), tag + " tke_max");
        assertNotEquals(2.8, turb.path("data").path("tke_max").asDouble(), 1e-9,
                tag + " tke_max 仍是旧硬编码 2.8");

        JsonNode vis = get(token, "/api/wrf/visibility?fileId=" + fileId);
        assertTrue(vis.path("success").asBoolean(), tag + " /visibility 失败: " + trim(vis.toString()));
        assertClose(risk.path("visibility").path("visibility_mean_m").asDouble(),
                vis.path("data").path("visibility_m").asDouble(), tag + " visibility_m");
        assertNotEquals(8500.0, vis.path("data").path("visibility_m").asDouble(), 1e-9,
                tag + " visibility_m 仍是旧硬编码 8500.0");

        JsonNode lig = get(token, "/api/wrf/lightning-risk?fileId=" + fileId);
        assertTrue(lig.path("success").asBoolean(), tag + " /lightning-risk 失败: " + trim(lig.toString()));
        assertClose(risk.path("convection").path("cape_max").asDouble(),
                lig.path("data").path("cape").asDouble(), tag + " cape");
        assertNotEquals(680.0, lig.path("data").path("cape").asDouble(), 1e-9,
                tag + " cape 仍是旧硬编码 680.0");

        System.out.println("[IT] " + tag + " C2: tke_max=" + turb.path("data").path("tke_max")
                + " visibility_m=" + vis.path("data").path("visibility_m")
                + " cape=" + lig.path("data").path("cape")
                + " risk_level=" + lig.path("data").path("risk_level").asText());
    }

    /** 换输入必须换输出：否则"从实际数据算出"不成立。 */
    private void assertC2Differs(String token, String fileIdA, String fileIdB) {
        double tkeA = get(token, "/api/wrf/turbulence?fileId=" + fileIdA).path("data").path("tke_max").asDouble();
        double tkeB = get(token, "/api/wrf/turbulence?fileId=" + fileIdB).path("data").path("tke_max").asDouble();
        double visA = get(token, "/api/wrf/visibility?fileId=" + fileIdA).path("data").path("visibility_m").asDouble();
        double visB = get(token, "/api/wrf/visibility?fileId=" + fileIdB).path("data").path("visibility_m").asDouble();
        double capeA = get(token, "/api/wrf/lightning-risk?fileId=" + fileIdA).path("data").path("cape").asDouble();
        double capeB = get(token, "/api/wrf/lightning-risk?fileId=" + fileIdB).path("data").path("cape").asDouble();
        System.out.println("[IT] 输入敏感的对照：tke " + tkeA + "→" + tkeB
                + " / vis " + visA + "→" + visB + " / cape " + capeA + "→" + capeB);
        assertTrue(tkeA != tkeB, "风场被放大后 tke_max 却没变 ⇒ 输出与输入无关（疑似常量）");
        assertTrue(visA != visB, "湿度/温度被改动后 visibility 却没变 ⇒ 疑似常量");
        assertTrue(capeA != capeB, "温度被改动后 cape 却没变 ⇒ 疑似常量");
    }

    // ── HTTP / 工具 ─────────────────────────────────────────────────

    /** 调 /parse（整文件 multipart），并把 fileId 塞进返回的 JsonNode 里一起带出。 */
    private JsonNode parseWholeFile(String token, byte[] raw, String fileName) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(raw) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> resp = rest.postForEntity("/api/wrf/parse?height=100",
                new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "/parse HTTP 状态: " + resp.getStatusCode());
        JsonNode node = json(resp.getBody());
        if (node.path("fileId").isTextual()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) node)
                    .put("_fileId", node.path("fileId").asText());
        }
        return node;
    }

    private JsonNode chunk(String token, String fileKey, String fileName, int index, int total,
                           String chunkHash, byte[] part) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chunkIndex", String.valueOf(index));
        body.add("totalChunks", String.valueOf(total));
        body.add("fileKey", fileKey);
        body.add("fileName", fileName);
        body.add("chunkHash", chunkHash);
        body.add("file", new ByteArrayResource(part) {
            @Override
            public String getFilename() {
                return fileName + ".part" + index;
            }
        });
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> resp = rest.postForEntity("/api/wrf/upload/chunk",
                new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(),
                "分片 " + index + " HTTP 状态: " + resp.getStatusCode() + " " + trim(resp.getBody()));
        return json(resp.getBody());
    }

    private JsonNode get(String token, String url) {
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "GET " + url + " → " + resp.getStatusCode());
        return json(resp.getBody());
    }

    private JsonNode postJson(String token, String url, Map<String, ?> payload) {
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "POST " + url + " → " + resp.getStatusCode());
        return json(resp.getBody());
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private static JsonNode json(String s) {
        try {
            return new JsonMapper().readTree(s);
        } catch (Exception e) {
            throw new AssertionError("返回体不是 JSON: " + trim(s), e);
        }
    }

    private static String jwt(String user) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user)
                .claim("roles", List.of("ROLE_USER"))
                .signWith(key)
                .compact();
    }

    /** 用 netCDF4 **独立**读样例文件的水平维度 —— 不拿实现给自己作证。 */
    private static int[] readNetcdfDims(Path nc) throws Exception {
        String code = "import netCDF4,sys,json;"
                + "d=netCDF4.Dataset(sys.argv[1]);"
                + "print(json.dumps([len(d.dimensions['south_north']),len(d.dimensions['west_east'])]))";
        Process pr = new ProcessBuilder(python.toString(), "-c", code, nc.toString())
                .redirectErrorStream(true).start();
        String out = new String(pr.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertTrue(pr.waitFor() == 0, "netCDF4 读取样例失败: " + out);
        JsonNode arr = json(out);
        return new int[]{arr.get(0).asInt(), arr.get(1).asInt()};
    }

    /** 造一份**输入不同**的样例（T2 +8 K、Q2 ×1.5、U/V ×1.35）用于"输出随输入变"的判据。 */
    private Path perturbedFixture() throws Exception {
        Path out = Paths.get("target", "it-fixtures", "perturbed_wrfout.nc").toAbsolutePath();
        Files.createDirectories(out.getParent());
        Path script = Paths.get("..", "..", "tools", "verify", "make_perturbed_wrfout.py")
                .toAbsolutePath().normalize();
        Process pr = new ProcessBuilder(python.toString(), script.toString(),
                fixture.toString(), out.toString())
                .redirectErrorStream(true).start();
        String log = new String(pr.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, pr.waitFor(), "生成扰动样例失败: " + log);
        return out;
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void assertClose(double expected, double actual, String what) {
        assertEquals(expected, actual, Math.max(1e-6, Math.abs(expected) * 1e-6),
                what + " 端点值必须等于同一次解析的 risk 块（expected=" + expected + " actual=" + actual + "）");
    }

    private static String trim(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() <= 600 ? s : s.substring(0, 600) + "…";
    }

    @SuppressWarnings("unused")
    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private static String join(List<String> xs) {
        return xs.stream().collect(Collectors.joining(","));
    }
}
