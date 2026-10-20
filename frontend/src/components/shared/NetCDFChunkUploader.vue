<template>
  <div class="netcdf-uploader">
    <el-card shadow="hover" class="uploader-card">
      <template #header>
        <div class="card-header">
          <span class="title">NetCDF 数据文件上传</span>
          <el-tag type="info" size="small">WRF 输出 · 支持断点续传</el-tag>
        </div>
      </template>

      <p class="desc">将 WRF 输出的 NetCDF 文件分片上传到后端解析。支持断点续传（页面刷新后恢复）。</p>

      <el-alert
        v-if="resumeInfo"
        :title="'检测到上次未完成上传：' + resumeInfo.fileName + '（已传 ' + resumeInfo.uploadedChunks + '/' + resumeInfo.totalChunks + ' 片）'"
        type="success"
        show-icon
        :closable="false"
        class="resume-alert"
      />

      <el-upload
        class="uploader"
        drag
        :auto-upload="false"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :on-change="onFileChange"
        accept=".nc,.nc4"
      >
        <div class="uploader-drag">
          <el-icon :size="48" color="#409EFF"><UploadFilled /></el-icon>
          <div class="drag-title">
            将文件拖拽到此处，或 <em>点击选择文件</em>
          </div>
          <div class="drag-hint">
            支持 <el-tag type="primary" size="small" effect="plain">.nc</el-tag>
            <el-tag type="primary" size="small" effect="plain">.nc4</el-tag>
            ，单文件最大 <el-tag type="warning" size="small" effect="plain">2 GB</el-tag>
          </div>
        </div>
      </el-upload>

      <el-form label-position="top" class="param-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="变量（必填）">
              <el-input v-model="params.variable" placeholder="例如：T2 / T / U" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时间步长">
              <el-row :gutter="8">
                <el-col :span="11">
                  <el-input-number v-model="params.timeStart" :min="0" :max="params.timeEnd" controls-position="right" style="width: 100%" />
                </el-col>
                <el-col :span="2" class="sep">~</el-col>
                <el-col :span="11">
                  <el-input-number v-model="params.timeEnd" :min="params.timeStart" :max="200" controls-position="right" style="width: 100%" />
                </el-col>
              </el-row>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="区域（经纬度范围）">
          <el-row :gutter="8">
            <el-col :span="6">
              <el-input-number v-model="params.lonMin" :min="-180" :max="params.lonMax" controls-position="right" placeholder="经度最小" style="width: 100%" />
            </el-col>
            <el-col :span="6">
              <el-input-number v-model="params.lonMax" :min="params.lonMin" :max="180" controls-position="right" placeholder="经度最大" style="width: 100%" />
            </el-col>
            <el-col :span="6">
              <el-input-number v-model="params.latMin" :min="-90" :max="params.latMax" controls-position="right" placeholder="纬度最小" style="width: 100%" />
            </el-col>
            <el-col :span="6">
              <el-input-number v-model="params.latMax" :min="params.latMin" :max="90" controls-position="right" placeholder="纬度最大" style="width: 100%" />
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item>
          <el-switch
            v-model="demoMode"
            active-text="仅演示（不实际上传，本地模拟）"
            inactive-text="实际上传"
          />
        </el-form-item>

        <!-- ★ 2026-09-28（C1 判据）：回退到 mock 必须是**显式**的。
             改前是「后端失败就悄悄换成 generateMockPayload()」，页面上看起来一样，
             只有一条 3 秒就消失的 warning —— 正是 C1 判据（"页面上出现真实解析结果，
             不是 mock"）想防的事。现在默认**不允许**回退：失败就是失败，
             错误信息留在页面上（alert 不会自动消失）。 -->
        <el-form-item v-if="!demoMode">
          <el-switch
            v-model="allowMockFallback"
            active-text="后端失败时允许回退演示数据（仅用于离线演示）"
            inactive-text="后端失败即报错（默认）"
          />
        </el-form-item>

        <el-alert
          v-if="lastError"
          type="error"
          show-icon
          :closable="false"
          class="fallback-alert"
          :title="'后端解析失败：' + lastError"
        />
        <el-alert
          v-if="fellBackToMock"
          type="warning"
          show-icon
          :closable="false"
          class="fallback-alert"
          title="⚠️ 当前页面显示的是**演示数据**（不是后端真实解析结果）—— 因为你显式打开了回退开关"
        />
      </el-form>

      <div class="progress-area" v-if="state !== 'idle' || file">
        <div class="progress-head">
          <div class="file-info">
            <el-tag type="success" size="small" effect="light" v-if="state === 'uploading'">上传中</el-tag>
            <el-tag type="warning" size="small" effect="light" v-else-if="state === 'paused'">已暂停</el-tag>
            <el-tag type="primary" size="small" effect="light" v-else-if="state === 'done'">完成</el-tag>
            <el-tag type="danger" size="small" effect="light" v-else-if="state === 'error'">错误</el-tag>
            <span class="file-name">{{ file?.name || '-' }}</span>
          </div>
          <div class="stat">
            <span>{{ formatBytes(uploadedBytes) }} / {{ formatBytes(totalBytes) }}</span>
          </div>
        </div>

        <el-progress
          :percentage="Math.round(percent)"
          :status="state === 'error' ? 'exception' : (state === 'done' ? 'success' : undefined)"
          :stroke-width="14"
        />

        <div class="sub-progress">
          <span>当前分片：{{ currentChunk }} / {{ totalChunks }} 片</span>
          <span class="speed">速度：{{ formatSpeed(speedKBs) }}</span>
          <span class="eta">剩余：{{ formatEta(etaSec) }}</span>
        </div>

        <div class="btns">
          <el-button type="primary" :disabled="!file || state === 'uploading' || state === 'done'" @click="startUpload">
            开始
          </el-button>
          <el-button type="warning" :disabled="state !== 'uploading'" @click="pauseUpload">
            暂停
          </el-button>
          <el-button type="success" :disabled="state !== 'paused'" @click="resumeUpload">
            继续
          </el-button>
          <el-button type="danger" :disabled="state === 'idle' || state === 'done'" @click="cancelUpload">
            取消
          </el-button>
        </div>
      </div>

      <div class="history" v-if="historyEntry">
        <el-divider content-position="left">最近一次上传</el-divider>
        <div class="history-item">
          <el-icon><Clock /></el-icon>
          <span>{{ historyEntry.fileName }} · {{ historyEntry.percent }}% · {{ historyEntry.time }}</span>
        </div>
      </div>

      <div class="cache-area" v-if="idbAvailable">
        <el-divider content-position="left">
          <span class="cache-title">
            <el-icon size="14"><UploadFilled /></el-icon>
            本地已有 {{ cachedFiles.length }} 个 NetCDF 文件
            <el-button
              link
              type="danger"
              size="small"
              style="margin-left: 8px"
              @click="clearNetCDFCache"
            >清理 NetCDF 缓存</el-button>
          </span>
        </el-divider>
        <div v-if="loadingCached" class="cache-loading">加载中...</div>
        <div v-else-if="cachedFiles.length === 0" class="cache-empty">暂无本地缓存文件</div>
        <div v-else class="cache-list">
          <div
            v-for="item in cachedFiles"
            :key="item.key"
            class="cache-item"
            @click="loadCachedFile(item)"
          >
            <div class="cache-item-main">
              <span class="cache-name">{{ item.name }}</span>
              <span class="cache-size">{{ formatBytes(item.size) }}</span>
            </div>
            <div class="cache-item-sub">
              <span v-if="item.uploadedAt">上传时间：{{ new Date(item.uploadedAt).toLocaleString() }}</span>
              <span v-if="item.expireAt && item.expireAt !== Infinity"> · 过期：{{ new Date(item.expireAt).toLocaleString() }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Clock } from '@element-plus/icons-vue'
import idb, { STORE_NETCDF } from '../../utils/indexedDB'
import { useTokenManager } from '../../stores/tokenManager'

const emit = defineEmits(['success', 'progress', 'error'])

const CHUNK_SIZE = 5 * 1024 * 1024
const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024
const MAX_RETRY = 3
const NETCDF_TTL = 7 * 24 * 60 * 60 * 1000 // 7 天

const file = ref(null)
const state = ref('idle')
// ★ 2026-09-26（C1）：默认由 true 改为 false。
//   改前默认走 mock（"安全默认"），结果是「真实链路」需要人工去关开关才可能被走到，
//   于是它从未被走到过。C1 的验收判据就是「关掉 demoMode 后页面上出现真实解析结果」，
//   而"关掉"这件事必须成为默认路径，否则下一个人还是不会碰到它。
//   仍保留开关：演示/离线环境可以显式切回纯本地模拟。
const demoMode = ref(false)

/**
 * ★ 2026-09-28（C1）：后端失败时**是否允许**回退到本地演示数据。默认 **false**。
 * 判据是「页面上出现真实解析结果（不是 mock）」⇒ 回退必须是显式选择，
 * 而且回退发生时页面上要留一条不会消失的告警（见模板里的 fallback-alert）。
 */
const allowMockFallback = ref(false)
/** 最近一次后端失败的原因；留在页面上，不自动消失。 */
const lastError = ref('')
/** 本次结果是否其实来自本地 mock（必须让用户看得见）。 */
const fellBackToMock = ref(false)

// ── 鉴权：复用项目既有的 token 存取工具（Pinia store `useTokenManager`）──
//   ★ 为什么必须补这一段：本组件的 6 处后端调用是**裸 fetch**，不走
//     `src/api/index.js` 里那个会自动注入 `Authorization` 的 axios 实例；
//     而服务端 `common/.../security/JwtSecurityConfig` 对 `/api/**` 要求
//     authenticated ⇒ 不带头直接 401。改前这条链路在真实服务上根本走不通。
const tokenManager = useTokenManager()

/** 取当前可用的 Bearer token（会先从 localStorage 恢复一次，幂等）。 */
function authHeaders () {
  try {
    tokenManager.restoreToken()
  } catch (e) {
    console.warn('[NetCDF] 恢复 token 失败:', e && e.message)
  }
  const t = tokenManager.token
  return t ? { Authorization: `Bearer ${t}` } : {}
}

// ★ 2026-09-26（C3）：分片 SHA-256 台账，合并时交给后端逐片复核。
const chunkHashes = ref({})

// ★★ 2026-10-01 新增：高度层默认值 —— **与后端 `WrfController.DEFAULT_HEIGHT_LAYERS`
//    以及 Python `wrf_processor.DEFAULT_LEVELS_M` 三处必须一致**。
//    修复前这三处分别是 12 / 9 / 1 层，同一个产品里"高度层"有三个答案。
//    ⚠️ 改这里也要同步改那两处（或反过来把三处收敛成从后端 API 取）。
const DEFAULT_HEIGHT_LEVELS = [2, 10, 50, 100, 200, 300, 500, 800, 1000, 1500, 2000, 3000]

// SHA-256 Web Worker 实例（分片哈希计算在 Worker 线程执行）
let hashWorker = null

function initHashWorker() {
  if (hashWorker) return
  try {
    hashWorker = new Worker(
      new URL('../../workers/sha256.worker.js', import.meta.url),
      { type: 'module' }
    )
    console.log('[NetCDF] SHA-256 Worker 已初始化')
  } catch (e) {
    console.warn('[NetCDF] Worker 初始化失败，回退到主线程:', e.message)
  }
}

/**
 * 使用 Web Worker 计算分片 SHA-256 哈希
 * @param {Blob} chunk - 文件分片
 * @param {number} index - 分片索引
 * @returns {Promise<string>} 十六进制哈希字符串
 */
function computeChunkHash(chunk, index) {
  return new Promise((resolve, reject) => {
    if (!hashWorker) {
      // Worker 不可用时回退到主线程
      chunk.arrayBuffer().then((buf) => {
        crypto.subtle.digest('SHA-256', buf).then((hashBuffer) => {
          const hashArray = Array.from(new Uint8Array(hashBuffer))
          resolve(hashArray.map((b) => b.toString(16).padStart(2, '0')).join(''))
        }).catch(reject)
      }).catch(reject)
      return
    }

    const onMessage = (e) => {
      hashWorker.removeEventListener('message', onMessage)
      if (e.data.success) {
        resolve(e.data.hash)
      } else {
        reject(new Error(e.data.error || '哈希计算失败'))
      }
    }

    hashWorker.addEventListener('message', onMessage)

    chunk.arrayBuffer().then((buf) => {
      hashWorker.postMessage({ chunk: buf, index }, [buf])
    }).catch((err) => {
      hashWorker.removeEventListener('message', onMessage)
      reject(err)
    })
  })
}

const params = reactive({
  variable: 'T2',
  timeStart: 0,
  timeEnd: 23,
  lonMin: 110,
  lonMax: 130,
  latMin: 20,
  latMax: 45
})

const totalChunks = ref(0)
const currentChunk = ref(0)
const uploadedChunks = ref(new Set())
const totalBytes = ref(0)
const uploadedBytes = ref(0)
const speedKBs = ref(0)
const etaSec = ref(0)

// 本地 NetCDF 缓存管理
const cachedFiles = ref([]) // [{ key, meta, size }]
const loadingCached = ref(false)
const idbAvailable = computed(() => !!idb && idb.isAvailable())

let pausedFlag = false
let cancelledFlag = false
let speedWindow = []
let fileKey = ''

const resumeInfo = ref(null)
const historyEntry = ref(null)

const percent = computed(() => {
  if (totalBytes.value === 0) return 0
  return Math.min(100, (uploadedBytes.value / totalBytes.value) * 100)
})

function formatBytes(b) {
  if (!b) return '0 B'
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(2) + ' KB'
  if (b < 1024 * 1024 * 1024) return (b / 1024 / 1024).toFixed(2) + ' MB'
  return (b / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

function formatSpeed(kbs) {
  if (!kbs || kbs <= 0) return '-'
  if (kbs < 1024) return kbs.toFixed(1) + ' KB/s'
  return (kbs / 1024).toFixed(2) + ' MB/s'
}

function formatEta(sec) {
  if (!sec || sec <= 0 || !isFinite(sec)) return '-'
  if (sec < 60) return Math.ceil(sec) + ' 秒'
  return Math.floor(sec / 60) + ' 分 ' + Math.ceil(sec % 60) + ' 秒'
}

/**
 * 生成断点续传用的 fileKey。
 *
 * ★ 2026-09-28（C3）修：后端 `WrfChunkUploadService.SAFE_KEY` 只接受
 *   `^[A-Za-z0-9_-]{1,128}$`（因为 fileKey 会被**直接当成磁盘目录名**，
 *   见 `WrfChunkUploadService.java:57-79`），而这里原来生成的是
 *   `名字.后缀_大小_时间戳` —— **含 `.`** ⇒ 真实链路上每个分片都被后端拒绝
 *   （HTTP 200 + `success:false`），前端重试 3 次后整单失败。
 *   实测证据：浏览器 E2E 日志里连续 3 条 `Receiving chunk 1/4 …` 然后无后续。
 *   ⚠️ 自写 harness 用的是 `harnessKey0001` / `nodecheck…` 这类**合法**键
 *   ⇒ 这个缺陷只有"真前端 + 真后端"才照得出来。
 *   现在：白名单化（保持确定性 ⇒ 同一文件仍映射到同一个 key，断点续传不受影响）。
 */
function genFileKey(f) {
  const raw = (f.name || 'file') + '_' + f.size + '_' + (f.lastModified || 0)
  return raw.replace(/[^A-Za-z0-9_-]/g, '_').slice(0, 128)
}

function saveState() {
  if (!fileKey) return
  const data = {
    fileKey,
    fileName: file.value.name,
    fileSize: file.value.size,
    totalChunks: totalChunks.value,
    uploadedChunks: Array.from(uploadedChunks.value),
    params: { ...params },
    percent: Math.round(percent.value),
    time: new Date().toLocaleString()
  }
  try {
    localStorage.setItem('netcdf_upload_state_' + fileKey, JSON.stringify(data))
    localStorage.setItem('netcdf_upload_last', JSON.stringify(data))
  } catch (e) {}
}

function loadResume(f) {
  try {
    const key = genFileKey(f)
    const raw = localStorage.getItem('netcdf_upload_state_' + key)
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

function loadHistory() {
  try {
    const raw = localStorage.getItem('netcdf_upload_last')
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

async function loadCachedFiles() {
  try {
    if (!idb.isAvailable()) return
    loadingCached.value = true
    const keys = await idb.keys(STORE_NETCDF)
    const list = []
    for (const k of keys) {
      try {
        const rec = await idb.get(STORE_NETCDF, k)
        if (rec && rec.blob) {
          list.push({
            key: k,
            name: (rec.meta && rec.meta.name) || String(k).replace(/^netcdf:/, ''),
            size: rec.blob.size || (rec.meta && rec.meta.size) || 0,
            expireAt: rec.expireAt,
            uploadedAt: (rec.meta && rec.meta.uploadedAt) || 0
          })
        }
      } catch (_) {}
    }
    cachedFiles.value = list
  } catch (e) {
    cachedFiles.value = []
  } finally {
    loadingCached.value = false
  }
}

async function loadCachedFile(item) {
  try {
    if (!idb.isAvailable()) return
    const rec = await idb.get(STORE_NETCDF, item.key)
    if (rec && rec.blob) {
      file.value = new File([rec.blob], item.name, { type: 'application/x-netcdf' })
      fileKey = item.key
      totalBytes.value = rec.blob.size
      totalChunks.value = Math.max(1, Math.ceil(rec.blob.size / CHUNK_SIZE))
      state.value = 'done'
      const mock = generateMockPayload()
      emit('success', mock)
      ElMessage.success('已从本地缓存加载文件：' + item.name)
    }
  } catch (e) {
    ElMessage.error('加载缓存文件失败')
  }
}

async function clearNetCDFCache() {
  try {
    if (!idb.isAvailable()) {
      ElMessage.info('当前环境不支持 IndexedDB')
      return
    }
    await idb.clear(STORE_NETCDF)
    cachedFiles.value = []
    ElMessage.success('NetCDF 缓存已清理')
  } catch (e) {
    ElMessage.error('清理失败：' + (e.message || ''))
  }
}

async function writeNetCDFToIDB(blob, name) {
  try {
    if (!idb.isAvailable()) return false
    const key = 'netcdf:' + (name || file.value ? file.value.name : '') + '_' + Date.now()
    const value = {
      blob,
      meta: { name: name || (file.value && file.value.name) || 'unknown.nc', size: blob.size, uploadedAt: Date.now(), profile: params.variable },
      expireAt: Date.now() + NETCDF_TTL,
      fetchedAt: Date.now()
    }
    await idb.put(STORE_NETCDF, key, value)
    return true
  } catch (e) {
    return false
  }
}

/**
 * ★ 2026-09-26（C1 浏览器联调抓出的**真 bug**）：
 *   `<el-upload :auto-upload="false">` 下 Element Plus **不会调用 `before-upload`**
 *   —— 那个钩子只在真正开始上传（或手动 `submit()`）时才触发。而这个组件既没有
 *   给 el-upload 挂 ref 去 submit()，也没有 `on-change`，于是：
 *       **选中文件后什么都不会发生**，`file` 永远是 null，
 *       `.progress-area` 的 `v-if="state !== 'idle' || file"` 恒为假
 *       ⇒「开始」按钮**根本不渲染** ⇒ 整条上传 + 后端解析链路
 *         **在 UI 上完全走不到**（连"演示模式"都点不动）。
 *   这解释了为什么「前端从不调后端」能一直藏着：不是没人想调，是调不到。
 *
 *   修法：用 `on-change` 把选文件事件接上（Element Plus 里 on-change 是
 *   (uploadFile, uploadFiles)，原始 File 在 `uploadFile.raw`）。
 *   `before-upload` 保留，作为将来改成 auto-upload 时的双保险。
 */
function onFileChange(uploadFile) {
  const raw = uploadFile && uploadFile.raw
  if (!raw) return
  beforeUpload(raw)
}

function beforeUpload(f) {
  if (f.size > MAX_FILE_SIZE) {
    ElMessage.error('文件大小超过 2GB 限制')
    return false
  }
  const name = (f.name || '').toLowerCase()
  if (!name.endsWith('.nc') && !name.endsWith('.nc4')) {
    ElMessage.error('仅支持 .nc / .nc4 文件')
    return false
  }
  file.value = f
  fileKey = genFileKey(f)
  totalBytes.value = f.size
  totalChunks.value = Math.ceil(f.size / CHUNK_SIZE)
  currentChunk.value = 0
  uploadedBytes.value = 0
  uploadedChunks.value = new Set()
  state.value = 'idle'
  speedKBs.value = 0
  etaSec.value = 0
  speedWindow = []
  pausedFlag = false
  cancelledFlag = false
  chunkHashes.value = {}

  const saved = loadResume(f)
  if (saved && saved.uploadedChunks && saved.uploadedChunks.length > 0 && saved.uploadedChunks.length < saved.totalChunks) {
    uploadedChunks.value = new Set(saved.uploadedChunks)
    uploadedBytes.value = uploadedChunks.value.size * CHUNK_SIZE
    currentChunk.value = uploadedChunks.value.size
    resumeInfo.value = {
      fileName: saved.fileName,
      uploadedChunks: saved.uploadedChunks.length,
      totalChunks: saved.totalChunks
    }
    Object.assign(params, saved.params || {})
    // ★ 2026-09-26（C3）：localStorage 只是**客户端记忆**，服务端可能早就被清理过
    //   （合并成功、或运维删了 chunks 目录）。真实模式下用后端状态校正一次，
    //   否则会「跳过」一批其实不存在的分片，合并必然失败。
    if (!demoMode.value) reconcileWithServer(f)
  } else {
    resumeInfo.value = null
  }
  return false
}

/** 用服务端 /upload/status 校正本地的断点记录（best-effort，不阻塞选文件）。 */
async function reconcileWithServer(f) {
  const key = genFileKey(f)
  const total = totalChunks.value
  try {
    const st = await fetchUploadStatus(key, total)
    const serverHas = new Set(st.receivedChunks || [])
    uploadedChunks.value = serverHas
    uploadedBytes.value = Math.min(totalBytes.value, serverHas.size * CHUNK_SIZE)
    currentChunk.value = serverHas.size
    resumeInfo.value = serverHas.size > 0 && serverHas.size < total
      ? { fileName: f.name, uploadedChunks: serverHas.size, totalChunks: total }
      : null
    console.log(`[NetCDF] 服务端已有 ${serverHas.size}/${total} 片`)
  } catch (e) {
    console.warn('[NetCDF] 从后端同步断点状态失败，沿用本地记录:', e.message)
  }
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

async function simulateUploadChunk(index) {
  let lastError = null
  for (let attempt = 1; attempt <= MAX_RETRY; attempt++) {
    if (cancelledFlag) return false
    try {
      const delay = 100 + Math.random() * 200
      await sleep(delay)
      if (Math.random() < 0.02 && attempt < MAX_RETRY) {
        throw new Error('network error')
      }
      return true
    } catch (e) {
      lastError = e
      await sleep(300 * attempt)
    }
  }
  throw lastError || new Error('上传失败')
}

function recordSpeed(bytes) {
  const now = Date.now()
  speedWindow.push({ bytes, time: now })
  speedWindow = speedWindow.filter(w => now - w.time < 3000)
  const totalB = speedWindow.reduce((s, w) => s + w.bytes, 0)
  const dt = Math.max(1000, speedWindow.length > 0 ? (now - speedWindow[0].time) : 1000)
  speedKBs.value = (totalB / 1024) / (dt / 1000)
  const remaining = totalBytes.value - uploadedBytes.value
  etaSec.value = speedKBs.value > 0 ? remaining / (speedKBs.value * 1024) : 0
}

function emitProgress() {
  emit('progress', Math.round(percent.value), speedKBs.value, etaSec.value, currentChunk.value, totalChunks.value)
}

async function startUpload() {
  if (!file.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!params.variable || !params.variable.trim()) {
    ElMessage.warning('请填写变量')
    return
  }
  pausedFlag = false
  cancelledFlag = false
  state.value = 'uploading'
  resumeInfo.value = null
  lastError.value = ''
  fellBackToMock.value = false

  try {
    for (let i = 0; i < totalChunks.value; i++) {
      if (cancelledFlag) break
      if (uploadedChunks.value.has(i)) continue
      while (pausedFlag && !cancelledFlag) {
        await sleep(150)
      }
      if (cancelledFlag) break

      currentChunk.value = i + 1
      if (demoMode.value) {
        await simulateUploadChunk(i)
      } else {
        const start = i * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, file.value.size)
        const blob = file.value.slice(start, end)
        await realUploadChunk(i, blob)
      }
      uploadedChunks.value.add(i)
      uploadedBytes.value = Math.min(totalBytes.value, uploadedChunks.value.size * CHUNK_SIZE)
      recordSpeed(CHUNK_SIZE)
      saveState()
      emitProgress()
    }

    if (!cancelledFlag) {
      state.value = 'done'
      // ★ 2026-09-24 接入真实后端解析。
      //   改动前这里是 `const payload = generateMockPayload()` —— 分片上传是模拟的
      //   （realUploadChunk 只 sleep 不发送），解析结果也是本地 mock
      //   ⇒ **整条 NetCDF 预览链路从未调用过任何后端**，
      //     后端 /api/wrf/parse 也因此一直没被验证过。
      //   现在：非演示模式走真实 POST；失败或演示模式回退到 mock，保证页面可用。
      // ★ 2026-09-26（C3）：真实模式下的首选路径改为
      //   「分片已真上传 → 后端合并 → 按 fileId 解析」，避免把同一份文件再传一遍；
      //   合并/按 fileId 解析失败才回退到 C1 那条整文件 POST。
      let payload = null
      if (!demoMode.value && file.value) {
        try {
          const merged = await mergeChunksOnBackend()
          payload = await parseUploadedViaBackend(merged.fileId)
          ElMessage.success('分片合并完成，已由后端解析 wrfout')
        } catch (e) {
          console.warn('[NetCDF] 分片合并/按 fileId 解析失败，改走整文件 POST:', e)
          try {
            payload = await parseViaBackend(file.value)
            ElMessage.success('已由后端解析 wrfout（整文件上传）')
          } catch (e2) {
            const reason = (e2 && e2.message) || String(e2)
            lastError.value = reason
            console.error('[NetCDF] 后端解析失败:', e2)
            if (!allowMockFallback.value) {
              // ★ 判据（C1）：页面上必须是**真实**解析结果。
              //   所以默认路径是"明确失败"：状态置 error、原因留在页面上
              //   （模板里的 error alert 不会自动消失），**不**静默换成 mock。
              state.value = 'error'
              emit('error', '后端解析失败：' + reason)
              ElMessage.error('后端解析失败：' + reason)
              return
            }
            // 只有显式打开开关才回退，而且页面上会挂一条不会消失的告警。
            fellBackToMock.value = true
            ElMessage.warning('后端解析失败，按显式开关回退演示数据：' + reason)
          }
        }
      }
      if (!payload) payload = generateMockPayload()
      try {
        localStorage.removeItem('netcdf_upload_state_' + fileKey)
      } catch (e) {}
      // 上传成功 → 写入 IndexedDB
      if (file.value && file.value instanceof Blob) {
        writeNetCDFToIDB(file.value, file.value.name).then(() => loadCachedFiles())
      }
      emit('success', payload)
      ElMessage.success('上传完成，已派发解析结果')
    }
  } catch (err) {
    state.value = 'error'
    emit('error', err.message || '上传失败')
    ElMessage.error(err.message || '上传失败')
  }
}

async function realUploadChunk(index, blob) {
  // 计算分片哈希（Worker 线程执行，不阻塞 UI）
  //
  // ★★ 2026-10-01 修：哈希失败**不再降级上传**。
  //    修复前是 `catch { console.warn(...) }` 然后照样 POST（不带 chunkHash），
  //    而服务端当时的校验写作 `expectedHash == null || … || 相等`
  //    ⇒ **不传哈希就整体跳过完整性校验**，C3 的「篡改被拒」判据静默失效。
  //    现在：算不出哈希 = 异常状态 ⇒ **硬失败并中止**，由调用方展示给用户。
  //    （服务端另有 `wrf.require-chunk-hash`（默认 true）做同一件事的第二道闸门。）
  if (!hashWorker) initHashWorker()
  let chunkHash
  try {
    chunkHash = await computeChunkHash(blob, index)
  } catch (e) {
    throw new Error(`分片 ${index} 的 SHA-256 计算失败：${e.message} —— 已中止上传（拒绝无校验分片）`)
  }
  if (!chunkHash) {
    throw new Error(`分片 ${index} 未取得 SHA-256 —— 已中止上传（拒绝无校验分片）`)
  }

  const fd = new FormData()
  fd.append('chunkIndex', index)
  fd.append('totalChunks', totalChunks.value)
  fd.append('fileKey', fileKey)
  fd.append('fileName', file.value.name)
  fd.append('chunkHash', chunkHash)
  fd.append('file', blob, `${file.value.name}.part${index}`)

  // ★ 2026-09-26（C3）真正的改动在这一段。
  //   改前这里是 `await sleep(100 + Math.random()*200); return` —— 组装好的
  //   FormData **从不发送**，所以「支持断点续传的分片上传」只是 UI 文案。
  //   现在打 POST /api/wrf/upload/chunk（后端已同步新增该端点）。
  let lastError = null
  for (let attempt = 1; attempt <= MAX_RETRY; attempt++) {
    if (cancelledFlag) throw new Error('已取消')
    try {
      const resp = await fetch('/api/wrf/upload/chunk', { method: 'POST', body: fd, headers: authHeaders() })
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
      const body = await resp.json()
      if (!body.success) throw new Error(body.error || '分片被后端拒绝')
      if (chunkHash) chunkHashes.value[index] = chunkHash
      return body
    } catch (e) {
      lastError = e
      if (attempt >= MAX_RETRY) break
      await sleep(300 * attempt)
    }
  }
  throw lastError || new Error('分片上传失败')
}

/**
 * 合并分片（★ 2026-09-26 C3 新增）。
 * 后端会逐片复核 SHA-256、复核总字节数，成功才落地并返回 fileId。
 */
async function mergeChunksOnBackend() {
  const resp = await fetch('/api/wrf/upload/merge', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({
      fileKey,
      fileName: file.value.name,
      totalChunks: totalChunks.value,
      chunkHashes: chunkHashes.value
    })
  })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  const body = await resp.json()
  if (!body.success) throw new Error(body.error || '合并失败')
  return body.data
}

/**
 * 按 fileId 让后端解析**它已经持有**的文件（★ 2026-09-26 C3 新增）。
 *
 * 为什么需要它：分片上传之后文件已经在服务器磁盘上了，再整文件 POST 一遍
 * 等于 2 倍流量。后端 `/parse-uploaded` 只接受 fileId，路径由服务端查库得到
 * （`/parse/advanced` 接受客户端给的 filePath，那是任意文件读取面，不能用）。
 * 返回结构与 `/parse` 一致，所以下面复用同一段解包逻辑。
 */
async function parseUploadedViaBackend(fileId) {
  const resp = await fetch('/api/wrf/parse-uploaded', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ fileId })
  })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  const body = await resp.json()
  if (!body.success) throw new Error(body.error || '后端处理失败')
  return unwrapParserPayload(body.data)
}

/** `/parse` 与 `/parse-uploaded` 的 data 都是 Python stdout 原始字符串 ⇒ 统一解包。 */
function unwrapParserPayload(data) {
  const parsed = typeof data === 'string' ? JSON.parse(data) : data
  if (!parsed || parsed.success !== true) {
    throw new Error((parsed && parsed.error) || '解析器返回失败')
  }
  if (!parsed.payload) throw new Error('返回里没有 payload 字段')
  return parsed.payload
}

/** 问后端「这个 fileKey 已经收到哪几片」——断点续传的真凭据在服务器上。 */
async function fetchUploadStatus(key, total) {
  const resp = await fetch(
    `/api/wrf/upload/status?fileKey=${encodeURIComponent(key)}&totalChunks=${total}`,
    { headers: authHeaders() })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  const body = await resp.json()
  if (!body.success) throw new Error(body.error || '查询失败')
  return body
}

/** 取消上传时清掉服务端的分片，别在磁盘上留一份等量垃圾。 */
async function discardChunksOnBackend(key) {
  try {
    await fetch(`/api/wrf/upload/chunk?fileKey=${encodeURIComponent(key)}`, { method: 'DELETE', headers: authHeaders() })
  } catch (e) {
    console.warn('[NetCDF] 清理服务端分片失败:', e.message)
  }
}

function pauseUpload() {
  if (state.value !== 'uploading') return
  pausedFlag = true
  state.value = 'paused'
  ElMessage.info('已暂停，下次刷新可恢复')
}

function resumeUpload() {
  if (state.value !== 'paused') return
  pausedFlag = false
  state.value = 'uploading'
  startUpload()
}

function cancelUpload() {
  cancelledFlag = true
  pausedFlag = false
  state.value = 'idle'
  // ★ 2026-09-26（C3）：真实模式下要同时清掉服务端的分片目录，
  //   否则每取消一次就在服务器上留一份等量垃圾（分片只有在 merge 时才清）。
  if (!demoMode.value && fileKey) {
    discardChunksOnBackend(fileKey)
  }
  file.value = null
  uploadedChunks.value = new Set()
  uploadedBytes.value = 0
  currentChunk.value = 0
  totalChunks.value = 0
  totalBytes.value = 0
  speedKBs.value = 0
  etaSec.value = 0
  try {
    if (fileKey) localStorage.removeItem('netcdf_upload_state_' + fileKey)
  } catch (e) {}
  ElMessage.info('已取消上传，断点记录已清除')
}

/**
 * 调后端解析 wrfout（★ 2026-09-24 新增）。
 *
 * 链路：前端 → Java /api/wrf/parse (multipart) → 子进程 wrf_processor.py → JSON
 * ⚠️ 两个必须知道的契约细节：
 *  ① Java 侧 `Map.of("success", true, "data", result, ...)` 里的 `data` 是
 *     **Python stdout 的原始字符串**，不是对象 ⇒ 必须 JSON.parse 一次。
 *     因为 `WrfController` 没有做反序列化，只是转发。
 *  ② 后端只接受 .nc/.netcdf，且 `height` 会被校验在 1~30000。
 *     `PythonScriptInvoker` 的默认超时是 30 s（`uav.python.timeout`），
 *     实测本解析器跑成都域 149×129 约 **1 秒**，余量充足。
 *
 * ★ 2026-09-26（C3）：真实模式下这已是**回退路径** —— 首选是「分片上传 → 合并 →
 *   按 fileId 解析」（`mergeChunksOnBackend` + `parseUploadedViaBackend`），
 *   只在合并/按 fileId 解析失败时才整文件 POST 一次。
 */
async function parseViaBackend (f) {
  const fd = new FormData()
  fd.append('file', f, f.name)
  // ★★ 2026-10-01 修（原为 `?height=100`，只返回 1 层）：
  //   后端 Python 的 `levels` **优先于** `height`，而传单个 `height` 会被
  //   `wrf_processor.py` 转成 `levels=[100.0]` ⇒ **只出 1 层**，页面高度层滑块
  //   因此只有一格。改传一组层即可，**不需要改 Python**。
  //   ⚠️ 这一组要与 `WrfController.DEFAULT_HEIGHT_LAYERS`（12 层）保持一致 ——
  //      同一个产品里"高度层"曾经有三个不同答案（Python 12 / Java 9 / 前端 1），
  //      现在统一到这一处。
  //   `height` 仍然保留：它是"安全参考面"的单值，后端用于 headline。
  const resp = await fetch('/api/wrf/parse?height=100&levels=' + DEFAULT_HEIGHT_LEVELS.join(','),
    { method: 'POST', body: fd, headers: authHeaders() })
  if (resp.status === 401) throw new Error('未登录或登录已过期（401）—— 请重新登录后再上传')
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  const body = await resp.json()
  if (!body.success) throw new Error(body.error || '后端处理失败')
  return unwrapParserPayload(body.data)
}

function generateMockPayload() {
  const latN = 30
  const lonN = 40
  const levelN = 10
  const lats = []
  const lons = []
  const levels = []
  const levelNames = []
  for (let i = 0; i < latN; i++) lats.push(+(params.latMin + (params.latMax - params.latMin) * i / (latN - 1)).toFixed(3))
  for (let j = 0; j < lonN; j++) lons.push(+(params.lonMin + (params.lonMax - params.lonMin) * j / (lonN - 1)).toFixed(3))
  const levelBases = [1000, 950, 900, 850, 800, 700, 600, 500, 400, 300]
  for (let k = 0; k < levelN; k++) {
    levels.push(levelBases[k])
    levelNames.push(levelBases[k] + 'hPa')
  }
  const temperature = []
  const u = []
  const v = []
  for (let k = 0; k < levelN; k++) {
    const t2d = []
    const u2d = []
    const v2d = []
    for (let i = 0; i < latN; i++) {
      const trow = []
      const urow = []
      const vrow = []
      for (let j = 0; j < lonN; j++) {
        const baseK = 290 - k * 4 - (latN - i) * 0.15 + Math.sin(j * 0.3) * 3
        const noise = (Math.random() - 0.5) * 2
        trow.push(+(baseK + noise).toFixed(2))
        urow.push(+(Math.sin(i * 0.2 + k) * 8 + (Math.random() - 0.5)).toFixed(2))
        vrow.push(+(Math.cos(j * 0.15 + k) * 6 + (Math.random() - 0.5)).toFixed(2))
      }
      t2d.push(trow)
      u2d.push(urow)
      v2d.push(vrow)
    }
    temperature.push(t2d)
    u.push(u2d)
    v.push(v2d)
  }
  return {
    fileName: file.value?.name || 'demo.nc',
    fileSize: file.value?.size || 42345678,
    dims: {
      time: params.timeEnd - params.timeStart + 1,
      level: levelN,
      lat: latN,
      lon: lonN
    },
    vars: [params.variable || 'T2', 'U', 'V', 'P', 'RH'],
    levelNames,
    grid: { lat: lats, lon: lons, levels },
    temperature,
    u,
    v
  }
}

historyEntry.value = loadHistory()

onMounted(() => {
  loadCachedFiles()
})

onBeforeUnmount(() => {
  if (hashWorker) {
    hashWorker.terminate()
    hashWorker = null
  }
})

onUnmounted(() => {
  cancelledFlag = true
})
</script>

<style scoped>
.netcdf-uploader {
  --primary: #409EFF;
}
.uploader-card {
  border-radius: 10px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header .title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
.desc {
  color: var(--color-text-muted);
  font-size: 13px;
  margin: 0 0 12px;
}
.resume-alert {
  margin-bottom: 12px;
}
.uploader :deep(.el-upload-dragger) {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #e4e7ed;
  border-radius: 6px;
  transition: border-color 0.2s;
  background: var(--color-bg);
}
.uploader :deep(.el-upload-dragger:hover) {
  border-color: var(--primary);
}
.uploader-drag {
  text-align: center;
}
.uploader-drag .drag-title {
  font-size: 15px;
  color: var(--color-text);
  margin: 12px 0 6px;
}
.uploader-drag .drag-title em {
  color: var(--primary);
  font-style: normal;
}
.uploader-drag .drag-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}
.uploader-drag .drag-hint .el-tag {
  margin-right: 4px;
}
.param-form {
  margin-top: 16px;
  background: var(--color-bg);
  padding: 12px 12px 4px;
  border-radius: 6px;
}
.param-form .sep {
  text-align: center;
  line-height: 32px;
  color: var(--color-text-muted);
}
.progress-area {
  padding: 16px;
  margin-top: 16px;
  background: var(--color-hover);
  border-radius: 8px;
  border: 1px solid var(--color-border);
}
.progress-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--color-text);
}
.progress-head .file-info .el-tag {
  margin-right: 8px;
}
.progress-head .file-info .file-name {
  font-family: monospace;
  color: var(--color-text-muted);
}
.sub-progress {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 12px;
  color: var(--color-text-muted);
  font-family: monospace;
}
.btns {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.history {
  margin-top: 4px;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-muted);
  font-family: monospace;
}

.cache-area {
  margin-top: 4px;
}
.cache-title {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  color: var(--color-text);
  font-weight: 500;
}
.cache-loading, .cache-empty {
  font-size: 12px;
  color: var(--color-text-muted);
  padding: 6px 4px;
}
.cache-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 2px;
  max-height: 260px;
  overflow-y: auto;
}
.cache-item {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-bg);
  cursor: pointer;
  transition: all 0.2s;
}
.cache-item:hover {
  border-color: #409EFF;
  background: var(--color-hover);
}
.cache-item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--color-text);
}
.cache-name {
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}
.cache-size {
  color: var(--color-text-muted);
  font-family: monospace;
}
.cache-item-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
