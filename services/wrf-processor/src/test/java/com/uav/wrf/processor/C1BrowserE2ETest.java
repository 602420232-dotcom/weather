package com.uav.wrf.processor;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ★ C1 的**浏览器端到端**判据（需要真实浏览器，默认跳过）。
 *
 * <p>这个测试做三件事：
 * <ol>
 *   <li>用**真实 Spring Boot**（DEFINED_PORT=8089，含 JwtSecurityConfig、Undertow、
 *       Spring MVC multipart、真 Python 子进程）把服务起起来；</li>
 *   <li>用同一密钥签一个 JWT，连同 vite dev server 的地址一起交给
 *       {@code tools/verify/e2e_c1_c3.mjs}；</li>
 *   <li>断言那个脚本的**全部判据通过**（脚本退出码 0），并把它的输出打进日志。</li>
 * </ol>
 *
 * <p>为什么必须单列：C1 的验收判据是「**关掉 demoMode 后页面上出现真实解析结果**」——
 * 判据的宾语是"页面"。HTTP 级/服务级测试都答不了这一问。
 *
 * <p>启用方式（默认 skip，且 skip **不算通过**）：
 * <pre>
 *   # 1) 另开一个终端起前端： cd frontend &amp;&amp; npm run dev   （监听 3000）
 *   # 2) mvn -pl common,services/wrf-processor test -Dtest=C1BrowserE2ETest \
 *   #        -Dsurefire.failIfNoSpecifiedTests=false -Dwrf.e2e.browser=true \
 *   #        -Dwrf.test.nc=&lt;wrfout.nc&gt;
 * </pre>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                // 与 application.yml / WrfPlatformIntegrationTest 对齐的**生产等价**配置
                "server.port=8089",                  // vite.config.js 里 /api 的 proxy target
                "spring.autoconfigure.exclude=",     // 解除测试 yml 对安全自动配置的排除
                "spring.main.allow-bean-definition-overriding=true",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:wrf_e2e;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.servlet.multipart.max-file-size=2048MB",
                "spring.servlet.multipart.max-request-size=2048MB",
                "wrf.python-script=wrf_processor.py",
                "wrf.data-path=target/e2e-data",
                "uav.python.enabled=true",
                "uav.python.timeout=300000",
                "uav.python.script-path=src/main/python",
                "uav.jwt.enabled=true",
                "uav.jwt.secret=" + C1BrowserE2ETest.JWT_SECRET
        })
class C1BrowserE2ETest {

    static final String JWT_SECRET =
            "e2e-only-secret-0123456789abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789";

    private static final String BASE = System.getProperty("wrf.e2e.base", "http://127.0.0.1:3000");

    /**
     * 解释器必须**显式指定**：自动探测只看 `--version`，不查 numpy/netCDF4，
     * 而本机 PATH 里第一个 `python3` 是 MSYS 的（没有 numpy）⇒ 解析必失败。
     */
    @org.springframework.test.context.DynamicPropertySource
    static void wirePython(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("uav.python.executable", () -> {
            Path p = WrfPlatformIntegrationTest.resolveUsablePython();
            return p == null ? "" : p.toString();
        });
    }

    @Test
    @DisplayName("C1 浏览器端到端：关掉 demoMode 后页面上出现真实解析结果（需要 vite + Playwright）")
    void browserEndToEnd() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("wrf.e2e.browser")
                        || "1".equals(System.getenv("WRF_E2E_BROWSER")),
                "浏览器 E2E 默认跳过（**跳过不等于通过**）：加 -Dwrf.e2e.browser=true 启用。");

        Path nc = fixture();
        Assumptions.assumeTrue(nc != null,
                "找不到样例 wrfout：设 -Dwrf.test.nc=<path> 或环境变量 WRF_TEST_NC。");

        Assumptions.assumeTrue(viteUp(),
                "vite dev server 未在 " + BASE + " 上监听 ⇒ 无法做浏览器联调"
                        + "（**跳过不等于通过**）。请先 `cd frontend && npm run dev`。");

        Path repoRoot = Paths.get("..", "..").toAbsolutePath().normalize();
        Path script = repoRoot.resolve("tools/verify/e2e_c1_c3.mjs");
        Path out = Paths.get("target", "e2e_result.txt").toAbsolutePath();
        assertTrue(Files.isRegularFile(script), "找不到浏览器脚本: " + script);

        ProcessBuilder pb = new ProcessBuilder(
                "node", script.toString(), BASE, nc.toString(), out.toString());
        pb.directory(repoRoot.toFile());
        pb.redirectErrorStream(true);
        pb.environment().put("WRF_E2E_TOKEN", jwt());
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        Process pr = pb.start();
        String output = new String(pr.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = pr.waitFor();
        System.out.println("──────── 浏览器 E2E 输出 ────────");
        System.out.println(output);
        System.out.println("──────── 输出结束（exit=" + exit + "）────────");
        assertEquals(0, exit, "浏览器 E2E 未全部通过（判据明细见上面的输出 / " + out + "）");
    }

    private static Path fixture() {
        String p = System.getProperty("wrf.test.nc");
        if (p == null) {
            p = System.getenv("WRF_TEST_NC");
        }
        if (p != null && Files.isRegularFile(Paths.get(p))) {
            return Paths.get(p).toAbsolutePath().normalize();
        }
        Path sample = Paths.get("src", "test", "resources", "verify", "sample_wrfout.nc")
                .toAbsolutePath().normalize();
        return Files.isRegularFile(sample) ? sample : null;
    }

    private static boolean viteUp() {
        // ★ 两个 host 都试：vite 默认监听的是 localhost，而 Java 侧可能先解析到 ::1
        //   （IPv6）再失败 —— 实测就踩到这一条（PowerShell/浏览器都能连，Java 不能）。
        for (String host : new String[]{"127.0.0.1", "localhost"}) {
            try {
                HttpURLConnection c = (HttpURLConnection) URI.create("http://" + host + ":3000/")
                        .toURL().openConnection();
                c.setConnectTimeout(3000);
                c.setReadTimeout(5000);
                c.setRequestMethod("GET");
                int code = c.getResponseCode();
                if (code > 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // 换下一个 host
            }
        }
        return false;
    }

    private static String jwt() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("e2e")
                .claim("roles", List.of("ROLE_USER"))
                .signWith(key)
                .compact();
    }
}
