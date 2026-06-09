# 中低危问题迭代重构方案

**版本**: v1.0  
**日期**: 2026-06-10  
**前提**: 严重和高危问题已在审计修复阶段全部处理完毕

---

## 总览

本方案覆盖审计报告中 9 个中危问题（7 项已完成骨架修复，剩余 4 项需架构级改进）和 7 个低危问题，按 4 个迭代周期推进，每个周期 1-2 周，总计约 6-8 周可全部完成。

| 迭代 | 周期 | 目标 | 涉及文件 |
|------|------|------|---------|
| 迭代一 | 第 1-2 周 | 组件状态标准化 + 错误处理体系 | 9 个共享组件、API 层 |
| 迭代二 | 第 3-4 周 | 大文件拆分 + CSS 统一 | DefaultLayout.vue、auth.js、NetCDFChunkUploader.vue |
| 迭代三 | 第 5-6 周 | WebSocket 统一 + 性能优化 | websocket.js、useWebSocket.ts、NetCDFChunkUploader.vue |
| 迭代四 | 第 7-8 周 | TypeScript 迁移 + CSP + 安全加固 | 全项目渐进迁移 |

---

## 迭代一：组件状态标准化 + 错误处理体系（中危）

### 目标

为所有共享组件补齐 loading / error / empty 三态，建立全局错误边界和结构化错误处理。

### 1.1 共享组件状态补齐

当前 9 个共享组件中有 5 个缺少完整的状态覆盖。按组件逐一处理：

**StatCard.vue**（约 80 行）

当前只有数据展示状态，缺少：
- `loading` 状态：显示 Element Plus `<el-skeleton>` 骨架屏
- `empty` 状态：显示空数据提示
- `error` 状态：显示错误信息及重试按钮

```vue
<!-- 建议改造后结构 -->
<template>
  <div class="stat-card">
    <el-skeleton v-if="loading" animated :rows="2" />
    <div v-else-if="error" class="stat-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="$emit('retry')">重试</el-button>
    </div>
    <div v-else-if="!hasData" class="stat-empty">
      <el-empty :description="$t('common.noData')" :image-size="60" />
    </div>
    <div v-else class="stat-content">
      <!-- 原有数据展示 -->
    </div>
  </div>
</template>
```

**Props 新增**：
- `loading: Boolean` (default: false)
- `error: String` (default: '')
- `empty: Boolean` (default: false)

**适用组件范围**：`GaugePanel.vue`、`DataScopeBadge.vue`、`ExcelBatchImporter.vue`（需补充导入失败恢复 UI）、`WeatherCard.vue`（组件和 shared 版两个副本）

---

### 1.2 全局错误边界组件

创建 `src/components/shared/ErrorBoundary.vue`，作为顶层错误捕获：

```vue
<!-- src/components/shared/ErrorBoundary.vue -->
<script setup>
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)
const errorMessage = ref('')

onErrorCaptured((err) => {
  hasError.value = true
  errorMessage.value = err.message || '未知错误'
  console.error('[ErrorBoundary]', err)
  return false // 阻止错误继续向上传播
})

function handleReset() {
  hasError.value = false
  errorMessage.value = ''
}
</script>

<template>
  <slot v-if="!hasError" />
  <div v-else class="error-boundary">
    <el-result icon="error" title="页面出现错误" :sub-title="errorMessage">
      <template #extra>
        <el-button type="primary" @click="handleReset">重试</el-button>
        <el-button @click="() => $router.push('/dashboard')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>
```

**集成方式**：在 `router/index.js` 的 `router-view` 外层包裹：

```vue
<ErrorBoundary>
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</ErrorBoundary>
```

---

### 1.3 结构化错误处理

创建 `src/utils/errorTypes.js`，定义错误分类体系：

```javascript
// src/utils/errorTypes.js
export class AppError extends Error {
  constructor(message, code, level = 'error') {
    super(message)
    this.name = 'AppError'
    this.code = code
    this.level = level // 'error' | 'warning' | 'info'
    this.timestamp = Date.now()
  }
}

export class NetworkError extends AppError {
  constructor(message = '网络连接失败，请检查网络后重试') {
    super(message, 'NETWORK_ERROR', 'error')
    this.name = 'NetworkError'
  }
}

export class AuthError extends AppError {
  constructor(message = '登录已过期，请重新登录') {
    super(message, 'AUTH_ERROR', 'error')
    this.name = 'AuthError'
  }
}

export class ValidationError extends AppError {
  constructor(message, field) {
    super(message, 'VALIDATION_ERROR', 'warning')
    this.name = 'ValidationError'
    this.field = field
  }
}

export class BusinessError extends AppError {
  constructor(message, code = 'BUSINESS_ERROR') {
    super(message, code, 'warning')
    this.name = 'BusinessError'
  }
}

// 错误类型 -> 用户提示映射
export const ERROR_MESSAGES = {
  NETWORK_ERROR: '网络连接失败，请检查网络后重试',
  AUTH_ERROR: '登录已过期，请重新登录',
  VALIDATION_ERROR: '输入数据校验失败',
  RATE_LIMIT: '操作过于频繁，请稍后再试',
  SERVER_ERROR: '服务器繁忙，请稍后重试',
  NOT_FOUND: '请求的资源不存在',
  FORBIDDEN: '您没有权限执行此操作',
}
```

在 API 层集成：修改 `src/api/index.js` 的 axios 响应拦截器，将 HTTP 状态码映射为对应的 AppError 子类。

---

### 1.4 迭代一交付物

| 交付物 | 文件 | 工作量 |
|--------|------|--------|
| StatCard 三态支持 | `components/shared/StatCard.vue` | 0.5 天 |
| GaugePanel/DataScopeBadge 状态补齐 | 对应组件文件 | 0.5 天 |
| ExcelBatchImporter 失败恢复 UI | `components/shared/ExcelBatchImporter.vue` | 0.5 天 |
| WeatherCard × 2 状态补齐 | 两个 WeatherCard 文件 | 0.5 天 |
| ErrorBoundary 组件 | `components/shared/ErrorBoundary.vue` | 0.5 天 |
| 错误分类体系 + API 层集成 | `utils/errorTypes.js`, `api/index.js` | 1 天 |
| 各视图接入 ErrorBoundary | `layouts/DefaultLayout.vue` | 0.5 天 |

---

## 迭代二：大文件拆分 + CSS 统一（低危）

### 目标

将 3 个单体大文件拆分为职责单一的模块，统一 CSS 方案。

### 2.1 DefaultLayout.vue 拆分（969 行 → 目标约 300 行）

当前单体文件承担了布局、导航、时间/天气/位置获取、主题切换等多种职责。

**拆分方案**：

```
src/layouts/
├── DefaultLayout.vue          # 主布局壳（~250 行，仅模板+导入）
├── components/
│   ├── LayoutHeader.vue       # 顶部栏（~150 行，含面包屑/模式标签/用户下拉）
│   ├── LayoutSidebar.vue      # 桌面端侧边栏（~100 行，含菜单+logo）
│   ├── LayoutMobileDrawer.vue # 移动端抽屉菜单（~80 行）
│   ├── LayoutFooter.vue       # 底部栏（~30 行）
│   ├── HeaderLocation.vue     # 位置信息小组件（~60 行）
│   ├── HeaderWeather.vue      # 天气信息小组件（~60 行）
│   └── HeaderClock.vue        # 时间信息小组件（~40 行）
```

**LayoutHeader.vue 示例接口**：

```vue
<script setup>
defineProps({
  isMobile: Boolean,
  currentTitle: String,
  locationText: String,
  isLocating: Boolean,
  currentTime: String,
  weatherIcon: String,
  weatherText: String,
  unreadCount: Number
})

defineEmits([
  'toggle-drawer', 'toggle-sidebar', 'toggle-theme',
  'open-notifications', 'fetch-location', 'user-command'
])
</script>
```

**HeaderClock.vue 示例**（纯展示组件，可独立测试）：

```vue
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { Clock } from '@element-plus/icons-vue'

const { t, locale } = useI18n()
const currentTime = ref('')
let timeInterval = null

function getWeekdayLabel(date) {
  const weekdayMap = {
    'zh-CN': ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
    'en-US': ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
    'ja-JP': ['日', '月', '火', '水', '木', '金', '土']
  }
  return (weekdayMap[locale.value] || weekdayMap['zh-CN'])[date.getDay()]
}

function updateTime() {
  const now = new Date()
  const h = now.getHours().toString().padStart(2, '0')
  const m = now.getMinutes().toString().padStart(2, '0')
  const y = now.getFullYear()
  const mo = (now.getMonth() + 1).toString().padStart(2, '0')
  const d = now.getDate().toString().padStart(2, '0')
  currentTime.value = `${y}-${mo}-${d} ${getWeekdayLabel(now)} ${h}:${m}`
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 30000)
})

onBeforeUnmount(() => {
  if (timeInterval) clearInterval(timeInterval)
})
</script>
```

**拆分后 DefaultLayout.vue** 只负责组合子组件，类似：

```vue
<template>
  <div class="uav-layout" :class="layoutClasses">
    <SkipLink />
    <DemoBanner v-if="authStore.demoMode" />
    
    <el-container class="uav-container">
      <LayoutSidebar v-if="!isMobile" />
      <el-container class="uav-main-container">
        <LayoutHeader v-bind="headerProps" @toggle-theme="appStore.toggleTheme()" />
        <el-main id="main-content">
          <ErrorBoundary>
            <router-view v-slot="{ Component }">
              <transition name="fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </ErrorBoundary>
        </el-main>
        <LayoutFooter />
      </el-container>
    </el-container>

    <LayoutMobileDrawer v-model="drawerVisible" />
    <NotificationDrawer v-model="notificationDrawerVisible" />
  </div>
</template>
```

---

### 2.2 auth.js 拆分（842 行 → 目标约 300 行）

当前 store 混合了权限矩阵、token 管理、用户状态、API 调用等多种职责。

**拆分方案**：

```
src/stores/
├── auth.js                    # 用户状态 + 登录/登出（~200 行）
├── permissions.js             # 权限矩阵 + 权限检查逻辑（~200 行）
├── tokenManager.js            # Token 生成/存储/刷新（~150 行）
└── constants/
    ├── roles.js               # ROLES / ROLE_LABELS / DEFAULT_ACCOUNTS（~80 行）
    └── permissionsMatrix.js   # PERMISSION_MATRIX / ACTION_PERMISSIONS（~200 行）
```

**permissions.js** 示例接口：

```javascript
// src/stores/permissions.js
import { defineStore } from 'pinia'
import { PERMISSION_MATRIX, ACTION_PERMISSIONS } from './constants/permissionsMatrix'

export const usePermissionStore = defineStore('permissions', () => {
  const matrix = ref(PERMISSION_MATRIX)

  function hasRouteAccess(role, routeKey) {
    return matrix.value[role]?.includes(routeKey) ?? false
  }

  function hasAction(role, actionKey) {
    return ACTION_PERMISSIONS[role]?.includes(actionKey) ?? false
  }

  function getAccessibleRoutes(role) {
    return matrix.value[role] || []
  }

  function mergeNacosMatrix(nacosData) {
    // Nacos 动态权限合并逻辑
  }

  function filterMenuItems(role, allItems) {
    return allItems.filter(item => hasRouteAccess(role, item.key))
  }

  return { matrix, hasRouteAccess, hasAction, getAccessibleRoutes, mergeNacosMatrix, filterMenuItems }
})
```

**tokenManager.js** 示例接口：

```javascript
// src/stores/tokenManager.js
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

export const useTokenManager = defineStore('tokenManager', () => {
  const token = ref(null)
  const payload = ref(null)
  const expiresAt = ref(0)
  let refreshTimer = null

  function setToken(tokenStr) { /* 现有逻辑 */ }
  function isTokenExpired(thresholdSec = 60) { /* 现有逻辑 */ }
  function refreshToken() { /* 现有逻辑 */ }
  function startTokenRefreshTimer() { /* 现有逻辑 */ }
  function stopTokenRefreshTimer() { /* 现有逻辑 */ }
  function persistToken() { /* localStorage 读写 */ }
  function restoreToken() { /* 从 localStorage 恢复 */ }
  function clearToken() { /* 清除 token */ }

  return { token, payload, expiresAt, setToken, isTokenExpired, refreshToken, clearToken, restoreToken }
})
```

---

### 2.3 CSS 方案统一

当前问题：项目中同时存在 scoped 样式、内联样式、`:deep()` 穿透样式三种写法。

**统一策略**：

第一步：将所有内联样式迁移到 scoped CSS（优先处理 DefaultLayout.vue 中模板的 inline style，如 `style="margin: 8px 0; border-color: rgba(255,255,255,0.1);"`）

第二步：在 `src/styles/` 下建立 CSS 变量体系，消除硬编码颜色值：

```css
/* src/styles/variables.css */
:root {
  /* 主题色 */
  --color-primary: #409eff;
  --color-primary-dark: #1e5ac8;
  --color-success: #52c41a;
  --color-warning: #e6a23c;
  --color-danger: #f56c6c;
  --color-info: #909399;

  /* 背景色 */
  --bg-primary: #f0f2f5;
  --bg-surface: #ffffff;
  --bg-sidebar: #001529;
  --bg-header: #ffffff;

  /* 文字色 */
  --text-primary: #24292f;
  --text-secondary: #666666;
  --text-muted: #8c8c8c;
  --text-inverse: #ffffff;

  /* 边框 */
  --border-color: #e5e7eb;
  --border-color-dark: #30363d;

  /* 尺寸 */
  --sidebar-width: 220px;
  --sidebar-collapsed: 64px;
  --header-height: 52px;
  --footer-height: 32px;

  /* 动画 */
  --transition-fast: 0.2s ease;
  --transition-normal: 0.3s ease;

  /* 圆角 */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
}

/* 暗色主题覆写 */
.is-dark {
  --bg-primary: #0d1117;
  --bg-surface: #161b22;
  --bg-header: #161b22;
  --text-primary: #c9d1d9;
  --text-secondary: #8b949e;
  --text-muted: #6e7681;
  --border-color: #30363d;
}
```

第三步：在现有 scoped 样式中逐步替换硬编码颜色值为 CSS 变量引用：

```css
/* 改造前 */
.uav-header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

/* 改造后 */
.uav-header {
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-color);
}
```

---

### 2.4 迭代二交付物

| 交付物 | 文件 | 工作量 |
|--------|------|--------|
| LayoutHeader.vue | `layouts/components/LayoutHeader.vue` | 1 天 |
| LayoutSidebar.vue | `layouts/components/LayoutSidebar.vue` | 0.5 天 |
| LayoutMobileDrawer.vue | `layouts/components/LayoutMobileDrawer.vue` | 0.5 天 |
| HeaderLocation/Weather/Clock 小组件 | 3 个文件 | 1 天 |
| DefaultLayout.vue 瘦身重构 | 现有文件 | 0.5 天 |
| permissions.js + tokenManager.js 拆分 | 2 个新 store 文件 | 1.5 天 |
| constants/roles.js + permissionsMatrix.js 提取 | 2 个常量文件 | 0.5 天 |
| CSS 变量体系 + 迁移 | `styles/variables.css`, 各组件样式 | 1.5 天 |

---

## 迭代三：WebSocket 统一 + 性能优化（中危）

### 目标

消除 WebSocket 重复实现，将 NetCDF 哈希计算移至 Web Worker，添加平板断点。

### 3.1 WebSocket 统一

当前状态分析：

| 文件 | 行数 | 角色 | 与真实 WS 关系 |
|------|------|------|---------------|
| `composables/useWebSocket.ts` | 69 | STOMP 基础层 | 真实连接 |
| `plugins/websocket.ts` | 22 | Vue 插件包装 | 封装 useWebSocket → 保留 |
| `utils/websocket.js` | 275 | 纯模拟通知服务 | 完全独立，零依赖 |

`plugins/websocket.ts` 是 `useWebSocket.ts` 的自然封装，不需要合并。需要处理的是 `utils/websocket.js`。

**方案**：将 `utils/websocket.js` 重构为基于 `useWebSocket` 的适配器，保留其通知业务逻辑，但底层连接改用 STOMP。

```
src/
├── composables/
│   └── useWebSocket.ts              # 不变：STOMP 基础层
├── plugins/
│   └── websocket.ts                 # 不变：Vue 插件封装
├── services/
│   └── notificationService.js       # 新建：通知业务逻辑（~180 行）
└── utils/
    └── websocket.js                 # 标记 @deprecated → 后续删除
```

**notificationService.js** 示例架构：

```javascript
// src/services/notificationService.js
import { useWebSocket } from '@/composables/useWebSocket'
import { getNotifications } from '@/api'

class NotificationService {
  #handlers = new Map()
  #connected = false

  async connect(userId, demoMode = false) {
    if (demoMode) {
      this.#startDemoSimulation(userId)
      return
    }
    const ws = useWebSocket()
    await ws.connect('/ws')
    ws.subscribe(`/user/${userId}/notifications`, (msg) => {
      this.#dispatch(msg)
    })
    this.#connected = true
  }

  #startDemoSimulation(userId) {
    // 模拟通知生成逻辑（保留原 utils/websocket.js 的业务逻辑）
    // 每隔 30-60 秒随机生成一条模拟通知
  }

  #dispatch(msg) {
    this.#handlers.forEach((handler) => handler(msg))
  }

  onNotification(handler) { /* ... */ }
  disconnect() { /* ... */ }
}

export const notificationService = new NotificationService()
```

---

### 3.2 NetCDF 哈希计算 Web Worker 化

`NetCDFChunkUploader.vue` 中 `crypto.subtle.digest('SHA-256')` 在主线程执行，大文件时可能阻塞 UI。提取到 Web Worker。

**步骤**：

第一步：创建 `src/workers/sha256.worker.js`：

```javascript
// src/workers/sha256.worker.js
self.onmessage = async (e) => {
  const { chunk, index } = e.data
  try {
    const hashBuffer = await crypto.subtle.digest('SHA-256', chunk)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
    self.postMessage({ index, hash: hashHex, success: true })
  } catch (err) {
    self.postMessage({ index, hash: null, success: false, error: err.message })
  }
}
```

第二步：在 `NetCDFChunkUploader.vue` 中使用 Worker：

```javascript
// 替换原有的 crypto.subtle.digest 调用

let hashWorker = null

function initHashWorker() {
  hashWorker = new Worker(
    new URL('@/workers/sha256.worker.js', import.meta.url),
    { type: 'module' }
  )
}

function computeHash(chunk, index) {
  return new Promise((resolve, reject) => {
    if (!hashWorker) initHashWorker()
    hashWorker.onmessage = (e) => {
      if (e.data.success) resolve(e.data.hash)
      else reject(new Error(e.data.error))
    }
    hashWorker.postMessage({ chunk, index })
  })
}

// 组件卸载时清理
onBeforeUnmount(() => {
  if (hashWorker) {
    hashWorker.terminate()
    hashWorker = null
  }
})
```

---

### 3.3 响应式断点扩展

在现有 768px 和 480px 基础上增加 1024px 平板断点：

```css
/* src/styles/breakpoints.css */

/* 平板断点（新增）：768px - 1024px */
@media (min-width: 769px) and (max-width: 1024px) {
  .uav-layout.is-tablet .uav-aside {
    width: 64px !important; /* 平板端折叠侧边栏 */
  }

  .uav-layout.is-tablet .logo-text {
    display: none;
  }

  .uav-layout.is-tablet .hide-on-tablet {
    display: none !important;
  }
}

/* 已有断点保持不变 */
@media (max-width: 768px) { /* 手机端：完全隐藏侧边栏 */ }
@media (max-width: 480px) { /* 小屏手机 */ }
```

在 `DefaultLayout.vue` 中添加表格模式检测：

```javascript
const isTablet = ref(false)
const isMobile = ref(false)

function updateBreakpoints() {
  const width = window.innerWidth
  isMobile.value = width <= 768
  isTablet.value = width > 768 && width <= 1024
}
```

---

### 3.4 迭代三交付物

| 交付物 | 文件 | 工作量 |
|--------|------|--------|
| notificationService.js | `services/notificationService.js` | 1.5 天 |
| websocket.js 标记 deprecated | 现有文件 | 0.5 天 |
| SHA-256 Web Worker | `workers/sha256.worker.js` | 0.5 天 |
| NetCDFChunkUploader Worker 集成 | 现有组件 | 0.5 天 |
| 平板断点 CSS + JS | `styles/breakpoints.css`, DefaultLayout.vue | 0.5 天 |

---

## 迭代四：TypeScript 渐进迁移 + CSP + 安全加固（低危）

### 目标

逐步提升 TypeScript 覆盖率，添加 CSP 头部，加固表单验证。

### 4.1 TypeScript 渐进迁移策略

采用自底向上策略，从低耦合高复用模块开始迁移：

**阶段 1**（第 1 周）：工具函数和常量

```
迁移目标：
├── src/utils/sanitize.js        → sanitize.ts（0.5 天）
├── src/utils/errorTypes.js      → errorTypes.ts（0.5 天）
├── src/utils/audit.js           → audit.ts（0.5 天）
├── src/utils/geolocation.js     → geolocation.ts（0.5 天）
└── src/utils/weatherApi.js      → weatherApi.ts（0.5 天）
```

**阶段 2**（第 2 周）：状态管理层

```
迁移目标：
├── src/stores/constants/*.js    → *.ts（1 天）
├── src/stores/permissions.js    → permissions.ts（1 天）
├── src/stores/tokenManager.js   → tokenManager.ts（1 天）
└── src/stores/app.js            → app.ts（0.5 天）
```

**迁移规范**：每个文件迁移时定义完整的接口类型：

```typescript
// src/stores/tokenManager.ts
interface TokenPayload {
  sub: string
  role: string
  demo?: boolean
  iat: number
  exp: number
}

interface StoredToken {
  token: string
  payload: TokenPayload
  expiresAt: number
}

interface RefreshResult {
  ok: boolean
  mode: 'demo' | 'prod' | 'failed'
  expiresAt: number
  reason?: string
}
```

---

### 4.2 Content Security Policy 配置

在 `vite.config.ts` 的 HTML 插件中注入 CSP meta 标签：

```typescript
// vite.config.ts 添加
import type { Plugin } from 'vite'

function cspPlugin(): Plugin {
  return {
    name: 'html-csp',
    transformIndexHtml(html) {
      return html.replace(
        '<head>',
        `<head>
    <meta http-equiv="Content-Security-Policy" content="
      default-src 'self';
      script-src 'self' 'unsafe-inline' 'unsafe-eval';
      style-src 'self' 'unsafe-inline';
      img-src 'self' data: https:;
      font-src 'self' data:;
      connect-src 'self' ws: wss: https:;
      frame-src 'none';
      object-src 'none';
      base-uri 'self';
      form-action 'self';
    ">`
      )
    }
  }
}
```

生产环境建议在 Nginx 层面配置 CSP 响应头以获得更强的安全性：

```nginx
# nginx.conf
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' https: wss:; frame-src 'none'; object-src 'none';" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

---

### 4.3 表单验证加固

**登录表单**（`src/views/auth/LoginView.vue`）增加规则：

```javascript
// 新增规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\-\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线、中划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码长度 6-128 个字符', trigger: 'blur' }
  ]
}
```

**注册表单**（`src/views/auth/RegisterView.vue`）密码强度校验：

```javascript
password: [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 8, message: '密码至少 8 个字符', trigger: 'blur' },
  {
    validator: (_rule, value, callback) => {
      const hasUpper = /[A-Z]/.test(value)
      const hasLower = /[a-z]/.test(value)
      const hasNumber = /[0-9]/.test(value)
      const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(value)
      const strength = [hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length
      if (strength < 2) {
        callback(new Error('密码需包含大写字母、小写字母、数字、特殊字符中至少两类'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }
]
```

---

### 4.4 迭代四交付物

| 交付物 | 文件 | 工作量 |
|--------|------|--------|
| 工具函数 TS 迁移（5 个文件） | `utils/*.ts` | 2.5 天 |
| Store 层 TS 迁移（4 个文件） | `stores/*.ts` | 3.5 天 |
| CSP meta 标签 + Nginx 配置 | `vite.config.ts`, nginx 配置 | 0.5 天 |
| 安全响应头 | Nginx/Caddy 配置 | 0.5 天 |
| 登录/注册表单验证加固 | `LoginView.vue`, `RegisterView.vue` | 0.5 天 |
| 密码强度组件 | 可复用 composable | 0.5 天 |

---

## 资源估算总览

| 迭代 | 周期 | 总工作人天 | 风险等级 | 前置依赖 |
|------|------|-----------|---------|---------|
| 迭代一 | 第 1-2 周 | 4 天 | 低 | 无 |
| 迭代二 | 第 3-4 周 | 8 天 | 中 | 迭代一（ErrorBoundary 需先就位） |
| 迭代三 | 第 5-6 周 | 3.5 天 | 中 | 迭代二（Store 拆分后 WebSocket 重构更安全） |
| 迭代四 | 第 7-8 周 | 8 天 | 低 | 迭代一、二完成后渐进推进 |
| **合计** | **8 周** | **23.5 天** | - | - |

实际并行度取决于团队人数。1 人独立推进约 5 周可完成全部迭代（部分迭代有串行依赖）。

---

## 风险与回滚策略

1. **大文件拆分**：每次拆分后在独立分支验证，保留原文件作为回退。每个子组件独立单元测试确保功能不退化。
2. **WebSocket 重构**：先保留 `utils/websocket.js` 标记 `@deprecated`，新 `notificationService.js` 在演示模式下复用旧的模拟逻辑，确认稳定后再删除旧文件。
3. **TypeScript 迁移**：按文件逐个迁移，不改动业务逻辑，仅添加类型标注。每个文件迁移后立即过 lint 和 build。
4. **CSS 变量迁移**：先建立变量文件，再逐组件替换。视觉回归测试通过截图对比确认无差异。
