// ★ C1 的**浏览器端到端**判据（Playwright + Chromium）。
//
// 链路：真实浏览器 → vite dev server(3000) → /api 代理 → **真实 Spring Boot**
//       （含 JwtSecurityConfig 过滤链、Spring MVC multipart、真 JPA/H2）
//       → 真 PythonScriptInvoker → 真 wrf_processor.py → 真 wrfout。
//
// 用法：
//   node tools/verify/e2e_c1_c3.mjs <baseUrl> <wrfout.nc> <out.txt>
// 需要环境变量 WRF_E2E_TOKEN = 与后端同一密钥签出的 JWT（由 C1BrowserE2ETest 注入）。
//
// ★ 与 2026-09-26 那版的区别（那版跑出 3 FAIL，且判据本身写错了）：
//   1. 字段位置 bug 已在 PlatformHttpServer 修掉；本版打的是**真实 Spring**，
//      不再经过自写 shim（旧版那 3 次 500 是 shim 从查询串取表单字段造成的）。
//   2. 旧版断言「高度层=12 且 纬度=129」——**解析器给不出这个组合**：
//      `?height=100` 只返回请求的那 1 层（不触发 auto_stride，网格保持原样）；
//      不带 height 才给 12 层，但 12 层会触发 auto_stride=2 ⇒ 网格抽稀。
//      本版按**实测语义**断言，并把 mock 不可能通过的性质写全。
//   3. 旧版用 ElMessage 弹窗当判据（3 秒自动消失 ⇒ flaky）；本版读 DOM。
import { chromium } from 'playwright'
import fs from 'node:fs'
import path from 'node:path'

const BASE = process.argv[2] || 'http://localhost:3000'
const WRF = process.argv[3]
const OUT = process.argv[4] || 'tools/verify/e2e_result.txt'
const TOKEN = process.env.WRF_E2E_TOKEN || ''
const log = []
function say(s) { console.log(s); log.push(s) }
let failures = 0
function check(name, ok, detail) {
  if (!ok) failures++
  say(`  ${ok ? '[PASS]' : '[FAIL]'} ${name}${detail !== undefined && detail !== null ? '   → ' + detail : ''}`)
}

const EXE = process.env.PW_CHROME ||
  'C:\\Users\\Dithiothreitol\\AppData\\Local\\ms-playwright\\chromium-1228\\chrome-win64\\chrome.exe'
const browser = await chromium.launch({ headless: true, executablePath: EXE })
const ctx = await browser.newContext({ viewport: { width: 1600, height: 1000 } })

// 种子登录态 + **真实 token**（本项目 token 存在 `uav_auth_token_v1`，见 stores/tokenManager.ts:8）
await ctx.addInitScript(([tok]) => {
  localStorage.setItem('uav_auth_user', JSON.stringify({
    id: 'e2e', username: 'e2e', role: 'admin', displayName: 'e2e'
  }))
  if (tok) {
    localStorage.setItem('uav_auth_token_v1', JSON.stringify({
      token: tok, payload: { sub: 'e2e', roles: ['ROLE_USER'] }, expiresAt: Date.now() + 3600 * 1000
    }))
  }
}, [TOKEN])

const page = await ctx.newPage()
const api = []
page.on('request', r => { if (r.url().includes('/api/wrf/')) api.push({ dir: 'REQ', m: r.method(), u: r.url(), auth: !!r.headers()['authorization'] }) })
page.on('response', r => { if (r.url().includes('/api/wrf/')) api.push({ dir: 'RES', s: r.status(), u: r.url() }) })
page.on('console', m => { if (m.type() === 'error') say('  [browser console error] ' + m.text()) })
page.on('pageerror', e => say('  [browser pageerror] ' + e.message))

try {
  say('== 0. 前置 ==')
  say('  base   = ' + BASE)
  say('  wrfout = ' + WRF + ' (' + fs.statSync(WRF).size + ' B)')
  check('E2E.0 已注入真实 JWT（否则后面必然 401）', !!TOKEN, TOKEN ? 'token 已注入' : '缺少 WRF_E2E_TOKEN')

  say('')
  say('== 1. 打开 /netcdf-preview ==')
  await page.goto(BASE + '/netcdf-preview', { waitUntil: 'domcontentloaded', timeout: 120000 })
  await page.waitForTimeout(2500)
  say('  URL = ' + page.url())
  check('C1-E2E.1 页面可达（未被重定向到 /login）', page.url().includes('/netcdf-preview'), page.url())
  const pageTitle = await page.locator('.page-title').first().innerText().catch(() => '')
  check('C1-E2E.2 渲染出 NetCDF 预览卡片', pageTitle.includes('NetCDF'), pageTitle)

  say('')
  say('== 2. demoMode 默认必须是「实际上传」==')
  const sw = page.locator('.param-form .el-switch').first()
  const swClass = (await sw.getAttribute('class')) || ''
  check('C1-E2E.3 demoMode 默认关闭（实际上是上传）', !swClass.includes('is-checked'),
    'class=' + swClass)

  say('')
  say('== 3. 选文件 ==')
  await page.locator('.uploader input[type=file]').setInputFiles(WRF)
  await page.waitForTimeout(1000)
  const fname = await page.locator('.file-name').innerText().catch(() => '')
  const chunkTxt = await page.locator('.sub-progress span').first().innerText().catch(() => '')
  say('  文件名 = ' + fname + ' | 分片 = ' + chunkTxt)
  check('C1-E2E.4 文件入队且被切分片', fname.includes('wrfout'), fname + ' | ' + chunkTxt)

  say('')
  say('== 4. 点「开始」→ 真实分片上传 + 合并 + 后端解析 ==')
  await page.locator('button:has-text("开始")').first().click()
  // 判据读 DOM（不读 3 秒消失的弹窗）：等页面头部出现「已加载文件」或错误 alert
  await page.waitForFunction(() => {
    const hdr = document.querySelector('.header-card .el-alert__title')
    const err = document.querySelector('.uploader-card .el-alert--error')
    return (hdr && hdr.textContent.includes('已加载文件')) || !!err
  }, { timeout: 300000 }).catch(() => say('  (等待超时，继续读当前 DOM 状态)'))
  await page.waitForTimeout(1500)

  const hdr = await page.locator('.header-card .el-alert__title').innerText().catch(() => '')
  say('  页面头部 = ' + hdr)
  check('C1-E2E.5 ★ 页面头部显示「已加载文件」（payload 真的进了页面）', hdr.includes('已加载文件'), hdr)
  const errAlert = await page.locator('.uploader-card .el-alert--error').innerText().catch(() => '')
  check('C1-E2E.6 没有出现后端失败告警', errAlert === '', errAlert)
  const mockAlert = await page.locator('.uploader-card .el-alert--warning').innerText().catch(() => '')
  check('C1-E2E.7 ★ 没有回退到演示数据（判据：页面上必须是真实结果）', mockAlert === '', mockAlert)

  say('')
  say('== 5. 读页面上真正渲染出来的维度/变量（mock 不可能通过）==')
  await page.locator('#tab-raw').click().catch(async () => { await page.locator('text=原始信息').first().click() })
  await page.waitForTimeout(1500)
  // 只在「文件与维度」那张表里取 label→value，避免被下面同名的「网格坐标」表覆盖
  const first = page.locator('.raw-info .el-descriptions').first()
  const labels = await first.locator('.el-descriptions__label').allInnerTexts()
  const values = await first.locator('.el-descriptions__content').allInnerTexts()
  const dims = {}
  labels.forEach((k, i) => { dims[k.trim()] = (values[i] || '').trim() })
  say('  「文件与维度」= ' + JSON.stringify(dims))
  check('C1-E2E.8 ★ 高度层=1（height=100 只请求 1 层；mock 给 10）', dims['高度层'] === '1', '高度层=' + dims['高度层'])
  check('C1-E2E.9 ★ 纬度=129（真实样例的水平维度；mock 给 30）', dims['纬度'] === '129', '纬度=' + dims['纬度'])
  check('C1-E2E.10 ★ 经度=149（mock 给 40）', dims['经度'] === '149', '经度=' + dims['经度'])
  check('C1-E2E.11 ★ 时间步=1（mock 给 24）', dims['时间步'] === '1', '时间步=' + dims['时间步'])

  const rawText = await page.locator('.raw-info').innerText()
  check('C1-E2E.12 ★ 变量是解析器真实输出 T/U/V/W/RH/WS（mock 给 T2/U/V/P/RH）',
    rawText.includes('WS') && rawText.includes('RH') && !rawText.includes('T2'),
    rawText.replace(/\n/g, ' | ').slice(0, 160))
  const levelTags = await page.locator('.raw-info .el-tag').allInnerTexts()
  say('  高度层标签 = ' + JSON.stringify(levelTags.slice(0, 6)))
  check('C1-E2E.13 ★ 高度层标签是 "…m AGL"（解析器口径；mock 给 hPa）',
    levelTags.some(t => t.includes('AGL')) && !levelTags.some(t => t.includes('hPa')),
    JSON.stringify(levelTags.slice(0, 4)))
  const snippet = await page.locator('.json-preview').innerText().catch(() => '')
  check('C1-E2E.14 原始 JSON 片段里出现真实维度 129×149',
    snippet.includes('129') && snippet.includes('149'), snippet.replace(/\s+/g, ' ').slice(0, 140))

  await page.locator('#tab-2d').click().catch(() => {})
  await page.waitForTimeout(2500)
  const canvasCount = await page.locator('.chart-box canvas').count()
  check('C1-E2E.15 2D 剖面图已渲染（ECharts canvas 存在）', canvasCount > 0, 'canvas=' + canvasCount)

  say('')
  say('== 6. 浏览器实际发出的 /api/wrf 请求 ==')
  api.forEach(a => say(`  ${a.dir} ${a.m || ''} ${a.s || ''} ${a.u}${a.dir === 'REQ' ? '  auth=' + a.auth : ''}`))
  const reqs = api.filter(a => a.dir === 'REQ')
  check('C3-E2E.1 浏览器真的发了 POST /upload/chunk', reqs.some(a => a.u.includes('/upload/chunk') && a.m === 'POST'), null)
  check('C3-E2E.2 浏览器真的发了 POST /upload/merge', reqs.some(a => a.u.includes('/upload/merge')), null)
  check('C3-E2E.3 浏览器真的发了 POST /parse-uploaded', reqs.some(a => a.u.includes('/parse-uploaded')), null)
  const res = api.filter(a => a.dir === 'RES')
  check('C3-E2E.4 ★ 所有 /api/wrf 响应都不是 401/500',
    res.length > 0 && res.every(a => a.s < 400),
    res.map(a => a.s).join(','))
  check('C3-E2E.5 ★ 每个请求都带了 Authorization 头（C1 的 JWT 修复）',
    reqs.length > 0 && reqs.every(a => a.auth), reqs.filter(a => !a.auth).map(a => a.u).join(',') || '全部带头')

  const shot = OUT.replace(/\.txt$/, '.png')
  await page.screenshot({ path: shot, fullPage: true })
  say('  截图 = ' + shot)
} catch (e) {
  failures++
  say('!! 异常：' + (e && e.stack ? e.stack : e))
} finally {
  // ★ 失败时也要把请求流水打出来 —— 否则"页面没出结果"这种失败无从定位
  //   （2026-09-28 就是靠它发现：分片被后端以 success:false 拒绝，HTTP 仍是 200）。
  say('')
  say('== 附：本次浏览器发出的 /api/wrf 请求流水 ==')
  if (api.length === 0) {
    say('  （没有发出任何 /api/wrf 请求）')
  }
  api.forEach(a => say(`  ${a.dir} ${a.m || ''} ${a.s || ''} ${a.u}${a.dir === 'REQ' ? '  auth=' + a.auth : ''}`))
  say('')
  say('================ 浏览器 E2E 判据汇总：' + (failures === 0 ? 'ALL PASS' : failures + ' FAIL') + ' ================')
  fs.mkdirSync(path.dirname(path.resolve(OUT)), { recursive: true })
  fs.writeFileSync(OUT, log.join('\n'), 'utf8')
  await browser.close()
  process.exit(failures === 0 ? 0 : 1)
}
