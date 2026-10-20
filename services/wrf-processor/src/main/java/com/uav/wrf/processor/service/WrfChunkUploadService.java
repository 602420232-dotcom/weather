package com.uav.wrf.processor.service;

import com.uav.wrf.processor.entity.WrfDataFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 分片上传的落盘与合并（<b>C3</b> 项的后端半边）。
 *
 * <p>★ 修复前的状态：前端 {@code NetCDFChunkUploader.vue} 的 {@code realUploadChunk()}
 * 组装好 FormData（chunkIndex / totalChunks / fileKey / fileName / chunkHash / file）
 * <b>然后只 {@code await sleep()}，从不发送</b>，而后端也没有任何 {@code chunk} 端点
 * （全仓库 grep 确认）。⇒ 「支持断点续传的分片上传」这句 UI 文案是<b>假的</b>。
 *
 * <p>★ 本类的设计取向是「**能证伪**」而不是「看起来很完整」：
 * <ul>
 *   <li>每个分片落成 {@code <dataPath>/chunks/<fileKey>/<index>.part}，
 *       {@code /upload/status} 直接<b>列目录</b>回答「收到了哪几片」，
 *       而不是靠内存里的 Set —— 服务重启后状态仍然真实（这才是断点续传的意义）。</li>
 *   <li>{@code chunkHash}（SHA-256 十六进制）不是装饰：合并时<b>逐片复核</b>，
 *       不一致直接拒绝，并把是第几片报出来。</li>
 *   <li>合并后复核<b>总字节数</b>与分片数；不通过不落地。</li>
 *   <li>{@code fileKey} 与 {@code fileName} 都做白名单化，
 *       与 {@code WrfController.parseWrfFile} 的路径遍历防护同一条纪律。</li>
 * </ul>
 *
 * @since 2026-09-26（平台侧 C3）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WrfChunkUploadService {

    /** 分片索引目录名。 */
    private static final String CHUNK_ROOT = "chunks";

    /** 只允许十六进制 + 短横线 + 下划线：fileKey 会直接当目录名，必须白名单化。 */
    private static final Pattern SAFE_KEY = Pattern.compile("^[A-Za-z0-9_-]{1,128}$");

    /** 单文件上限 2 GB，与前端 {@code MAX_FILE_SIZE} 对齐。 */
    public static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024;

    /** 分片数上限，防"声称 10 亿片"这类请求。 */
    public static final int MAX_TOTAL_CHUNKS = 8192;

    @Value("${wrf.data-path:./data}")
    private String dataPath;

    /**
     * ★★ 2026-10-01 新增：**是否强制要求分片哈希**（默认 {@code true}）。
     *
     * <p><b>为什么加这个开关</b>：修复前 {@code chunkHash} 是
     * {@code @RequestParam(required = false)}，且本类的校验写作
     * {@code expectedHash == null || expectedHash.isBlank() || …}
     * ⇒ <b>不传哈希就整体跳过完整性校验</b>。
     * 结果是 C3 的「篡改被拒」判据**只在哈希在场时成立**，而"哈希不在场"
     * 恰恰是攻击者（或一个前端 bug）最容易制造的状态。
     *
     * <p><b>为什么不直接把 {@code required} 改成 true</b>：那是**破坏性**的 ——
     * 任何未升级的老客户端会立刻 400。所以走开关过渡：
     * 默认严格，需要兼容旧客户端时显式关掉（配置项
     * {@code wrf.require-chunk-hash=false}）。
     *
     * <p>★ 与 {@code WrfRiskMetrics} 的 {@code caveat} 同一条纪律：
     * <b>校验失效这件事必须能被看见</b>，不能静默降级。
     */
    @Value("${wrf.require-chunk-hash:true}")
    private boolean requireChunkHash;

    // ══════════════════════════════════════════════════════════════════════
    //  入参校验
    // ══════════════════════════════════════════════════════════════════════

    /** 校验 fileKey。抛 {@link IllegalArgumentException} 时由 Controller 转成 400。 */
    public static String requireSafeKey(String fileKey) {
        if (fileKey == null || !SAFE_KEY.matcher(fileKey).matches()) {
            throw new IllegalArgumentException(
                    "fileKey 必须是 1~128 位的 [A-Za-z0-9_-]，收到：" + fileKey);
        }
        return fileKey;
    }

    /**
     * 校验并归一化文件名：只保留基名，去掉任何目录成分，只接受 .nc / .nc4 / .netcdf。
     *
     * <p>注意这里与 {@code WrfController.parseWrfFile} 的策略不同：那里是
     * 「含 {@code ..} 或路径分隔符就拒绝」，这里因为要拼盘落盘、还要展示给人看，
     * 改为「取基名 + 扩展名白名单」。
     */
    public static String requireSafeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName 不能为空");
        }
        String base = fileName.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        String lower = base.toLowerCase();
        if (!lower.endsWith(".nc") && !lower.endsWith(".nc4") && !lower.endsWith(".netcdf")) {
            throw new IllegalArgumentException("仅支持 .nc / .nc4 / .netcdf，收到：" + fileName);
        }
        // 只保留可打印的 ASCII 安全字符，中文名等一律替换，避免落盘出怪文件
        String safe = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank() || safe.startsWith(".")) {
            throw new IllegalArgumentException("文件名不合法：" + fileName);
        }
        return safe;
    }

    public static void requireChunkIndex(int chunkIndex, int totalChunks) {
        if (totalChunks < 1 || totalChunks > MAX_TOTAL_CHUNKS) {
            throw new IllegalArgumentException("totalChunks 必须在 1~" + MAX_TOTAL_CHUNKS + " 之间");
        }
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException(
                    "chunkIndex 必须在 0~" + (totalChunks - 1) + " 之间");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  1. 收分片
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 落一个分片。返回 {success, fileKey, chunkIndex, totalChunks, bytes, sha256}。
     *
     * <p>幂等：同一 (fileKey, chunkIndex) 重复上传就是覆盖，前端重试天然安全。
     */
    public Map<String, Object> saveChunk(String fileKey, String fileName, int chunkIndex,
                                         int totalChunks, String expectedHash,
                                         MultipartFile chunk) throws IOException {
        requireSafeKey(fileKey);
        requireSafeFileName(fileName);
        requireChunkIndex(chunkIndex, totalChunks);
        if (chunk == null || chunk.isEmpty()) {
            throw new IllegalArgumentException("分片内容为空");
        }
        requireInsideDataPath(fileKey, chunkIndex);

        Path dir = chunkDir(fileKey);
        Files.createDirectories(dir);
        Path target = dir.resolve(chunkIndex + ".part");

        // 先写临时文件再原子改名：避免半个分片被 status/merge 看见
        Path tmp = dir.resolve(chunkIndex + ".part.tmp");
        byte[] data = chunk.getBytes();
        Files.write(tmp, data);
        Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String actualHash = sha256Hex(data);
        boolean hashPresent = !(expectedHash == null || expectedHash.isBlank());

        //   ★★ 2026-10-01 修：**缺哈希不再等于通过**。
        //     修复前 `hashOk` 的第一项就是 `expectedHash == null` ⇒ 不传就放行。
        //     现在：开了 `wrf.require-chunk-hash`（默认 true）时，
        //     **没有哈希直接拒**，并把原因说清（而不是静默收下一个未经校验的分片）。
        if (!hashPresent && requireChunkHash) {
            Files.deleteIfExists(target);
            throw new IllegalArgumentException(
                    "分片 " + chunkIndex + " 未携带 SHA-256（chunkHash）。"
                    + "服务端已启用 wrf.require-chunk-hash ⇒ 拒绝未经完整性校验的分片。"
                    + "若需兼容旧客户端，请显式设置 wrf.require-chunk-hash=false。");
        }
        boolean hashOk = !hashPresent || expectedHash.equalsIgnoreCase(actualHash);
        if (!hashOk) {
            Files.deleteIfExists(target);
            throw new IllegalArgumentException(
                    "分片 " + chunkIndex + " 的 SHA-256 与前端声明不符（传输损坏？）");
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("fileKey", fileKey);
        m.put("fileName", fileName);
        m.put("chunkIndex", chunkIndex);
        m.put("totalChunks", totalChunks);
        m.put("bytes", data.length);
        m.put("sha256", actualHash);
        m.put("hashVerified", hashPresent);
        m.put("hashRequired", requireChunkHash);
        m.put("receivedChunks", receivedChunks(fileKey, totalChunks).size());
        return m;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  2. 查询已收到的分片（断点续传的真凭据）
    // ══════════════════════════════════════════════════════════════════════

    /** 列出已落盘的分片索引（升序）。目录不存在 ⇒ 空列表。 */
    public List<Integer> receivedChunks(String fileKey, Integer totalChunks) throws IOException {
        requireSafeKey(fileKey);
        Path dir = chunkDir(fileKey);
        if (!Files.isDirectory(dir)) {
            return new ArrayList<>();
        }
        List<Integer> idx = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.forEach(p -> {
                String name = p.getFileName().toString();
                if (name.endsWith(".part")) {
                    try {
                        idx.add(Integer.parseInt(name.substring(0, name.length() - 5)));
                    } catch (NumberFormatException ignored) {
                        // 不是分片命名，忽略
                    }
                }
            });
        }
        idx.sort(Comparator.naturalOrder());
        if (totalChunks != null) {
            idx.removeIf(i -> i < 0 || i >= totalChunks);
        }
        return idx;
    }

    public Map<String, Object> status(String fileKey, Integer totalChunks) throws IOException {
        requireSafeKey(fileKey);
        List<Integer> got = receivedChunks(fileKey, totalChunks);
        long bytes = 0L;
        Path dir = chunkDir(fileKey);
        if (Files.isDirectory(dir)) {
            for (Integer i : got) {
                bytes += Files.size(dir.resolve(i + ".part"));
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("fileKey", fileKey);
        m.put("receivedChunks", got);
        m.put("receivedCount", got.size());
        m.put("receivedBytes", bytes);
        if (totalChunks != null) {
            m.put("totalChunks", totalChunks);
            m.put("complete", got.size() == totalChunks);
            List<Integer> missing = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                if (!got.contains(i)) {
                    missing.add(i);
                }
            }
            m.put("missingChunks", missing);
        }
        return m;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  3. 合并
    // ══════════════════════════════════════════════════════════════════════

    /** 合并结果：落盘路径 + 字节数 + sha256（可用于与前端整文件哈希比对）。 */
    public static final class MergeResult {
        public final Path path;
        public final long bytes;
        public final String sha256;

        MergeResult(Path path, long bytes, String sha256) {
            this.path = path;
            this.bytes = bytes;
            this.sha256 = sha256;
        }
    }

    /**
     * 合并分片成完整 NetCDF。
     *
     * <p>校验三条：① 分片齐全且编号连续；② 总字节数 ≤ {@link #MAX_FILE_SIZE}；
     * ③ 每片的 SHA-256 与前端声明一致（若声明了）。
     * 任一条不通过<b>不落地</b>，且返回里指出是第几片出的问题。
     */
    public MergeResult merge(String fileKey, String fileName, int totalChunks,
                             Map<Integer, String> expectedHashes) throws IOException {
        requireSafeKey(fileKey);
        String safeName = requireSafeFileName(fileName);
        if (totalChunks < 1 || totalChunks > MAX_TOTAL_CHUNKS) {
            throw new IllegalArgumentException("totalChunks 必须在 1~" + MAX_TOTAL_CHUNKS + " 之间");
        }

        List<Integer> got = receivedChunks(fileKey, totalChunks);
        if (got.size() != totalChunks) {
            List<Integer> missing = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                if (!got.contains(i)) {
                    missing.add(i);
                }
            }
            throw new IllegalStateException("分片不齐：缺 " + missing);
        }

        Path dir = chunkDir(fileKey);
        Path out = dataRoot().resolve(safeName).normalize();
        if (!out.startsWith(dataRoot())) {
            throw new IllegalArgumentException("路径遍历攻击检测");
        }
        Files.createDirectories(out.getParent());

        MessageDigest whole = newDigest();
        long written = 0L;
        //   ★★ 2026-10-01 新增：开了 `wrf.require-chunk-hash` 时，
        //      **合并阶段必须有覆盖全部分片的哈希**，缺任何一片就拒。
        //      修复前是「传了才查」⇒ 逐片校验可以**整体缺席**（见 saveChunk 的注释）。
        //      ★ 注意这里必须**先整体检查再合并**，不能边合并边发现缺 —— 否则
        //        已经写了一半的 `.merging` 临时文件要靠 catch 清理，容易漏。
        if (requireChunkHash) {
            List<Integer> noHash = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                String want = (expectedHashes == null) ? null : expectedHashes.get(i);
                if (want == null || want.isBlank()) {
                    noHash.add(i);
                }
            }
            if (!noHash.isEmpty()) {
                throw new IllegalStateException(
                        "合并被拒：以下分片没有 SHA-256 声明 " + noHash
                        + "（服务端已启用 wrf.require-chunk-hash）。"
                        + "请让客户端对每个分片都计算并提交 chunkHash。");
            }
        }
        //   ★ 说明：合并阶段**无法反推"上传时到底校过没有"**（分片只落字节，
        //     当时是否带哈希没有留痕）。所以这里只能验"合并请求是否给了全量哈希"。
        //     ★ "上传时缺哈希"那道闸门在 `saveChunk` 里（默认开启）——
        //       两道闸门分工：saveChunk 管"收的时候有没有校"，merge 管"合的时候齐不齐"。
        Path tmp = out.resolveSibling(out.getFileName() + ".merging");
        try (OutputStream os = Files.newOutputStream(tmp)) {
            for (int i = 0; i < totalChunks; i++) {
                Path part = dir.resolve(i + ".part");
                byte[] bytes = Files.readAllBytes(part);
                if (expectedHashes != null && expectedHashes.containsKey(i)) {
                    String want = expectedHashes.get(i);
                    String actual = sha256Hex(bytes);
                    if (want != null && !want.isBlank() && !want.equalsIgnoreCase(actual)) {
                        throw new IllegalStateException(
                                "分片 " + i + " 的 SHA-256 不符：声明 " + want + "，实测 " + actual);
                    }
                }
                written += bytes.length;
                if (written > MAX_FILE_SIZE) {
                    throw new IllegalStateException("合并后超过 " + MAX_FILE_SIZE + " 字节上限");
                }
                os.write(bytes);
                whole.update(bytes);
            }
        } catch (RuntimeException | IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        Files.move(tmp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // ★ 合并成功后清掉分片目录：否则每次上传都会在磁盘上留一份等量的垃圾
        deleteChunks(fileKey);

        return new MergeResult(out, written, toHex(whole.digest()));
    }

    /** 删掉某 fileKey 的全部分片（前端"取消上传"也应调它）。 */
    public int deleteChunks(String fileKey) throws IOException {
        requireSafeKey(fileKey);
        Path dir = chunkDir(fileKey);
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        int n = 0;
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : s.toList()) {
                Files.deleteIfExists(p);
                n++;
            }
        }
        Files.deleteIfExists(dir);
        return n;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  4. 与 DB 的衔接
    // ══════════════════════════════════════════════════════════════════════

    /** 合并后的文件登记成 {@link WrfDataFile}，返回给前端当后续 parse 的 fileId。 */
    public WrfDataFile register(WrfDataService dataService, MergeResult merged, String originalName) {
        return dataService.createWrfDataFile(
                originalName != null ? originalName : merged.path.getFileName().toString(),
                merged.path.toString(), merged.bytes);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  小工具
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ★ 2026-09-28 修：`wrf.data-path` 一律**先绝对化**再用。
     *
     * <p>为什么必须这样：配置里写的是相对路径（生产 `application.yml` 是 `./data`），
     * 而相对路径在"落盘"这一步有**两个不同的解释器**：
     * <ul>
     *   <li>Java NIO（本类、`Files.*`）按 **JVM 工作目录** 解释；</li>
     *   <li>Servlet 容器（Undertow 的 `Part.write` / Spring `transferTo`）按
     *       **Servlet 规范的 multipart 临时目录**解释。</li>
     * </ul>
     * 两者不一致 ⇒ 实测浏览器 E2E 里 `POST /parse` 抛
     * `NoSuchFileException: <java.io.tmpdir>/undertow.8089.…/target/e2e-data/…`
     * ——`Files.createDirectories` 建的是工作目录下的 `target/e2e-data`，
     * 而 Undertow 往它自己的临时目录里同名相对路径写。
     * ⇒ 绝对化之后两边指向同一处，这个坑从根上消失。
     */
    private Path dataRoot() {
        return Paths.get(dataPath == null || dataPath.isBlank() ? "." : dataPath)
                .toAbsolutePath().normalize();
    }

    Path chunkDir(String fileKey) {
        Path base = dataRoot().resolve(CHUNK_ROOT).normalize();
        Path dir = base.resolve(fileKey).normalize();
        // 双保险：requireSafeKey 已经排掉分隔符，这里再确认一次没跳出 chunks 根
        if (!dir.startsWith(base)) {
            throw new IllegalArgumentException("fileKey 越界");
        }
        return dir;
    }

    private void requireInsideDataPath(String fileKey, int chunkIndex) {
        Path dir = chunkDir(fileKey);
        Path p = dir.resolve(chunkIndex + ".part").normalize();
        if (!p.startsWith(dataRoot())) {
            throw new IllegalArgumentException("分片路径越界");
        }
    }

    /** 供测试/诊断：把 dataPath 暴露出来（Spring 注入的是私有字段）。 */
    public Map<String, Object> describeConfig() {
        Map<String, Object> m = new HashMap<>();
        m.put("dataPath", dataPath);
        m.put("dataPathAbsolute", dataRoot().toString());
        m.put("chunkRoot", dataRoot().resolve(CHUNK_ROOT).toString());
        m.put("maxFileSizeBytes", MAX_FILE_SIZE);
        m.put("maxTotalChunks", MAX_TOTAL_CHUNKS);
        return m;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 SHA-256", e);
        }
    }

    /** 流式计算一个文件的 SHA-256（合并前的整文件复核用）。 */
    public static String sha256Hex(Path file) throws IOException {
        MessageDigest md = newDigest();
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = is.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        }
        return toHex(md.digest());
    }

    public static String sha256Hex(byte[] data) {
        return toHex(newDigest().digest(data));
    }

    private static String toHex(byte[] digest) {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
