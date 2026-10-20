// C1/C3 的 HTTP 级回归检查（模拟前端 NetCDFChunkUploader.vue 的**真实请求形态**）。
//
// ★ 为什么需要它：2026-09-26 那次浏览器 E2E 拿到 3 次 500，根因是
//   PlatformHttpServer 从**查询串**取 chunkIndex/totalChunks/fileKey/fileName，
//   而组件是把它们放进 **FormData**（NetCDFChunkUploader.vue:656-664）。
//   本脚本用 Node 的 FormData/fetch 复刻浏览器那一份 multipart（含字段位置），
//   因此它**能**抓到那个 bug，也**能**证明它被修好。
//
// 用法： node tools/verify/http_c1c3_check.mjs <wrfout.nc> [baseUrl] [expectLat] [expectLon]
import fs from 'node:fs'
import crypto from 'node:crypto'
import path from 'node:path'

const NC = process.argv[2]
const BASE = process.argv[3] || 'http://127.0.0.1:8089'
const EXP_LAT = process.argv[4] ? Number(process.argv[4]) : null
const EXP_LON = process.argv[5] ? Number(process.argv[5]) : null

const CHUNK_SIZE = 5 * 1024 * 1024   // 与组件里的常量一致
let failures = 0
const lines = []
function say(s) { console.log(s); lines.push(s) }
function check(name, ok, detail) {
  if (!ok) failures++
  say(`  ${ok ? '[PASS]' : '[FAIL]'} ${name}${detail !== undefined && detail !== null ? '   → ' + detail : ''}`)
}
const sha256 = (buf) => crypto.createHash('sha256').update(buf).digest('hex')

const fileBuf = fs.readFileSync(NC)
const fileName = path.basename(NC)
const fileKey = 'nodecheck' + Date.now().toString(36)
const total = Math.max(1, Math.ceil(fileBuf.length / CHUNK_SIZE))

say(`== 输入 ==`)
say(`  file   = ${NC}`)
say(`  bytes  = ${fileBuf.length}`)
say(`  sha256 = ${sha256(fileBuf)}`)
say(`  分片   = ${total} × ${CHUNK_SIZE} B，fileKey=${fileKey}`)

// ── 1. 逐片上传（字段全部放在 FormData 里，与前端一致）──
say('')
say('== 1. POST /api/wrf/upload/chunk（字段在 FormData 里）==')
const chunkHashes = {}
for (let i = 0; i < total; i++) {
  const blob = new Blob([fileBuf.subarray(i * CHUNK_SIZE, Math.min((i + 1) * CHUNK_SIZE, fileBuf.length))])
  const fd = new FormData()
  fd.append('chunkIndex', String(i))
  fd.append('totalChunks', String(total))
  fd.append('fileKey', fileKey)
  fd.append('fileName', fileName)
  const h = sha256(Buffer.from(await blob.arrayBuffer()))
  fd.append('chunkHash', h)
  fd.append('file', blob, `${fileName}.part${i}`)
  const resp = await fetch(`${BASE}/api/wrf/upload/chunk`, { method: 'POST', body: fd })
  const body = await resp.json().catch(() => ({}))
  check(`C3-HTTP.${i + 1} 分片 ${i} 落盘 200 且 success=true`,
    resp.status === 200 && body.success === true,
    `HTTP ${resp.status} ${JSON.stringify(body).slice(0, 120)}`)
  if (body.success) chunkHashes[i] = h
}

// ── 2. status ──
say('')
say('== 2. GET /api/wrf/upload/status ==')
const st = await (await fetch(`${BASE}/api/wrf/upload/status?fileKey=${fileKey}&totalChunks=${total}`)).json()
check('C3-HTTP.status 报告收齐且 complete=true',
  st.success === true && st.complete === true && st.receivedCount === total,
  JSON.stringify(st).slice(0, 160))

// ── 3. merge ──
say('')
say('== 3. POST /api/wrf/upload/merge ==')
const mg = await (await fetch(`${BASE}/api/wrf/upload/merge`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ fileKey, fileName, totalChunks: total, chunkHashes })
})).json()
check('C3-HTTP.merge 成功', mg.success === true, JSON.stringify(mg).slice(0, 160))
check('C3-HTTP.merge 产物 sha256 == 输入 sha256（逐位一致）',
  mg.success === true && mg.data.sha256 === sha256(fileBuf),
  mg.success ? mg.data.sha256 : 'n/a')
const fileId = mg.success ? mg.data.fileId : null

// ── 4. parse-uploaded（按 fileId 解析服务端已持有的文件）──
say('')
say('== 4. POST /api/wrf/parse-uploaded ==')
if (fileId) {
  const pu = await (await fetch(`${BASE}/api/wrf/parse-uploaded`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fileId })
  })).json()
  const payload = pu.success ? JSON.parse(pu.data).payload : null
  check('C1-HTTP.parse-uploaded 成功且 data 是 Python stdout 原始字符串',
    pu.success === true && typeof pu.data === 'string', `HTTP data type=${typeof pu.data}`)
  if (payload) reportPayload(payload, 'parse-uploaded')
} else {
  check('C1-HTTP.parse-uploaded 前序步骤失败，跳过', false, 'merge 未返回 fileId')
}

// ── 5. 整文件 /parse（C1 的回退路径）──
say('')
say('== 5. POST /api/wrf/parse?height=100（整文件 multipart）==')
{
  const fd = new FormData()
  fd.append('file', new Blob([fileBuf]), fileName)
  const resp = await fetch(`${BASE}/api/wrf/parse?height=100`, { method: 'POST', body: fd })
  const body = await resp.json()
  const payload = body.success ? JSON.parse(body.data).payload : null
  check('C1-HTTP./parse 成功', resp.status === 200 && body.success === true,
    `HTTP ${resp.status} ${JSON.stringify(body).slice(0, 120)}`)
  if (payload) reportPayload(payload, '/parse')
  // C2：三个诊断端点（走同一条 Python 链路）
  const probeFileId = body.fileId || fileId
  say('')
  say('== 6. C2 三个诊断端点（fileId=' + probeFileId + '）==')
  for (const [ep, key, hard] of [['turbulence', 'tke_max', 2.8], ['visibility', 'visibility_m', 8500.0], ['lightning-risk', 'cape', 680.0]]) {
    const r = await (await fetch(`${BASE}/api/wrf/${ep}?fileId=${probeFileId}`)).json()
    const v = r.success ? r.data[key] : null
    check(`C2-HTTP./${ep} 成功且 ${key} 不等于旧的硬编码 ${hard}`,
      r.success === true && v !== null && Number(v) !== hard,
      r.success ? `${key}=${v}` : JSON.stringify(r).slice(0, 140))
  }
}

function reportPayload(payload, tag) {
  const dims = payload.dims || {}
  const vars = payload.vars || payload.variables || []
  const levelNames = payload.levelNames || []
  say(`  payload.dims=${JSON.stringify(dims)} vars=${JSON.stringify(vars)} levelNames[0]=${levelNames[0]}`)
  // ★ 判据必须与**实测**对齐，不能凭印象写：
  //   - `?height=100` ⇒ 解析器只返回请求的那 1 层（wrf_processor.py:1096-1111 FIX-HEIGHT），
  //     且 1 层不会触发 auto_stride，于是水平网格保持 129×149 原样；
  //   - 不带 height ⇒ 走 DEFAULT_LEVELS_M 的 12 层，但 12 层会触发 auto_stride=2
  //     ⇒ 网格被抽稀成 65×75。
  //   ⇒ 「level=12 且 lat=129」这个组合**解析器永远给不出来**（上一轮 E2E 就是这么写的，
  //     实测为假）。这里断言的是 height=100 这条真实链路。
  //   mock 给的是 level=10 / lat=30 / lon=40 / hPa / 变量含 T2 ⇒ 下面每条都区分得开。
  check(`C1-HTTP.${tag} 高度层=1 且是 "100m AGL"（height=100 只返回请求层；mock 给 10 层 hPa）`,
    Number(dims.level) === 1 && String(levelNames[0] || '').includes('100m AGL'),
    `level=${dims.level} levelNames[0]=${levelNames[0]}`)
  check(`C1-HTTP.${tag} 变量含 WS/RH 且不含 T2（mock 给 T2/U/V/P/RH）`,
    vars.includes('WS') && vars.includes('RH') && !vars.includes('T2'), JSON.stringify(vars))
  if (EXP_LAT) check(`C1-HTTP.${tag} 纬度=${EXP_LAT}（mock 给 30）`, Number(dims.lat) === EXP_LAT, 'lat=' + dims.lat)
  if (EXP_LON) check(`C1-HTTP.${tag} 经度=${EXP_LON}（mock 给 40）`, Number(dims.lon) === EXP_LON, 'lon=' + dims.lon)
  // 温度必须是真实 K 值域（mock 是 15~30 °C 的随机数）
  const t0 = payload.temperature && payload.temperature[0]
  if (t0) {
    const flat = t0.flat(3).filter(v => typeof v === 'number')
    const mn = Math.min(...flat), mx = Math.max(...flat)
    check(`C1-HTTP.${tag} 第一层温度是真实 K 值域（mock 是 15~30 的"摄氏度"随机数）`,
      mn > 200 && mx < 330, `min=${mn.toFixed(2)} max=${mx.toFixed(2)}`)
  }
}

say('')
say(`================ HTTP 级判据汇总：${failures === 0 ? 'ALL PASS' : failures + ' FAIL'} ================`)
fs.writeFileSync(process.argv[1].replace(/\.mjs$/, '_result.txt'), lines.join('\n'), 'utf8')
process.exit(failures === 0 ? 0 : 1)
