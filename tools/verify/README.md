# tools/verify —— 平台侧 C1/C2/C3 的验证装置（**不在 .gitignore 里**）

> 2026-09-28 建立。此前这套东西放在 `services/wrf-processor/target/_verify/`，
> 而 `target/` 被 `.gitignore:55` 忽略 ⇒ **一次 `mvn clean` 全部消失**，
> 连"上一轮到底测过什么"都无从复核。

## 0. 一句话结论（各判据的证据在哪）

| 判据 | 结论 | 证据 |
|---|---|---|
| C1 前端浏览器端到端 | ✅ 已达成（真浏览器 + 真 Spring + 真 wrfout） | `evidence/browser_e2e_result.txt`、`evidence/browser_e2e_screenshot.png` |
| C1 HTTP/服务级 | ✅ 通过 | 见下 `run_http_harness.ps1` 的输出；`evidence/http_check_result.txt` |
| C2 三个诊断端点 | ✅ 三项都由真实 wrfout 算出（数值 == 解析器 risk 块，且随输入变） | `services/wrf-processor/src/test/.../WrfPlatformIntegrationTest.java`（`[IT]` 日志） |
| C3 分片上传 | ✅ 端点齐备 + 前端真发字节 + 哈希不匹配会被拒 | 同上 + `http_c1c3_check.mjs` |

## 1. 目录内容

| 文件 | 作用 |
|---|---|
| `PlatformC2C3Harness.java` | 直接调用 `WrfController` 的 C2/C3 判据（含真实 wrfout 的输入→输出） |
| `PlatformHttpServer.java` | 把生产 `WrfController` 架在 JDK HttpServer 上供浏览器/HTTP 打（**不是**真实 Spring 上下文，见文件头注释） |
| `run_http_harness.ps1` | 解析 Python → `mvn compile` → `javac` → 起 `PlatformHttpServer`（8089） |
| `http_c1c3_check.mjs` | 用 Node 的 `FormData/fetch` **复刻前端请求形态**做 HTTP 级回归（就是它抓出了 shim 的 500） |
| `e2e_c1_c3.mjs` | **浏览器**端到端（Playwright + Chromium），读 DOM 判据 + 截图 |
| `make_sample_wrfout.py` | 从真实 wrfout 裁一份小样例（进仓库，供集成测试离线跑） |
| `make_perturbed_wrfout.py` | 造一份**输入不同**的副本，用于"输出随输入变"的判据 |
| `evidence/` | 已跑过的结果留档（结果 txt + 截图） |

## 2. 怎么重跑

### 2.1 单元 + 集成测试（不需要浏览器）

```powershell
cd <repo>
mvn -pl common,services/wrf-processor test
```

- 集成测试 `WrfPlatformIntegrationTest` 用 `src/test/resources/verify/sample_wrfout.nc`
  （裁切版，2.4 MB）跑**真实 Spring 上下文**（真 JwtSecurityConfig、真 Undertow multipart、
  真 Python 子进程）。
- 想跑**完整 20 MB 真实 wrfout**：`$env:WRF_TEST_NC='<path>\wrfout_d01_....nc'` 再执行上面的命令。
- 需要 Python 解释器（必须带 `numpy` + `netCDF4`）。解析顺序：
  `-Dwrf.test.python=` → `WRF_TEST_PYTHON` → `WRF_PYTHON` →
  `D:\Developer\workplace\vrp-env\Scripts\python.exe` → `D:\Developer\Anaconda\python.exe`。
  **找不到就跳过（不算通过）**。

### 2.2 HTTP 级 harness（不需要浏览器）

```powershell
# 终端 A：起 harness（8089）
pwsh -File tools/verify/run_http_harness.ps1 -Nc <wrfout.nc>
# 终端 B：跑判据
node tools/verify/http_c1c3_check.mjs <wrfout.nc> http://127.0.0.1:8089 129 149
```

### 2.3 浏览器端到端（需要前端 dev server）

```powershell
# 终端 A：前端（必须绑 127.0.0.1：vite 默认只监听 ::1，Java 侧的连通性检查会失败）
cd frontend; npm run dev -- --host 127.0.0.1
# 终端 B：真实 Spring（8089，vite 的 /api 代理目标）+ 真浏览器
cd <repo>
mvn -pl common,services/wrf-processor test -Dtest=C1BrowserE2ETest `
    -Dsurefire.failIfNoSpecifiedTests=false -Dwrf.e2e.browser=true `
    -Dwrf.test.nc=<wrfout.nc>
```

判据明细会打在 surefire 日志里，同时写到
`services/wrf-processor/target/e2e_result.txt`（+ 同名 `.png` 截图）。

## 3. 生成/裁切样例

```powershell
# 从真实 wrfout 裁一份小样例（默认 ny=36 nx=44，约 2.4 MB）
python tools/verify/make_sample_wrfout.py <真实 wrfout> services/wrf-processor/src/test/resources/verify/sample_wrfout.nc
```

## 4. 已知的口径（别照抄旧结论）

1. **`?height=100` 只返回 1 层**，不是 12 层。`12 层` 是不带 `height` 时的默认层数，
   而 12 层会触发 `auto_stride=2`，水平网格被抽稀（129×149 → 65×75）。
   ⇒ 「高度层=12 **且** 纬度=129」这个组合解析器**给不出来**；
   2026-09-26 那版 E2E 就是这么写的，必然失败。
2. **判据要用与浏览器等价的解析器**：Jackson 默认容忍 JSON 尾随内容，
   而浏览器是严格的 `JSON.parse` —— 用 Jackson 判会漏掉"stderr 混进 stdout"这类缺陷。
3. **相对路径是坑**：`uav.python.script-path` 与 `wrf.data-path` 现在都在使用处绝对化。
   Undertow 的 `Part.write` 按 Servlet 规范的 multipart 临时目录解释相对路径，
   而 Java NIO 按 JVM 工作目录解释 —— 两者混用会出现"建目录的地方 ≠ 写文件的地方"。
