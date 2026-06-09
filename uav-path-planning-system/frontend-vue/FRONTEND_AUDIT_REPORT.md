# UAV 路径规划系统 - 前端全面审计报告

**审计日期**: 2026-06-10  
**审计范围**: frontend-vue (Vue 3 + Vite + Element Plus + Pinia)  
**审计方法**: 静态代码分析 + 架构审核 + 安全扫描  
**版本**: v3.3.0

---

## 执行摘要

本次审计对 UAV 路径规划系统的前端代码库进行了全面评估，涵盖 UI/UX、代码质量、性能、功能和安全五个维度。项目整体架构设计合理，采用了 Vue 3 Composition API、Pinia 状态管理和 Element Plus 组件库，路由懒加载和国际化（zh-CN/en-US/ja-JP）均已实现。主要发现包括：3 个严重安全漏洞（token 明文存储于 localStorage 且无签名验证、v-html 无消毒渲染用户内容、innerHTML 可执行脚本）、40 余处硬编码中文文本绕过 i18n、以及跨组件缺少 loading/error/empty 状态处理。共计发现 1 个严重问题、5 个高危问题、9 个中危问题和 7 个低危改进项。

---

## 一、安全审计

### 1.1 Token 以明文存储在 localStorage（严重）

**文件**: `src/stores/auth.js` 第 506-513 行

```javascript
localStorage.setItem(TOKEN_KEY, JSON.stringify({
  token: tokenStr,
  payload,
  expiresAt
}))
```

**问题描述**: JWT token 及 payload（包含用户名、角色等敏感信息）以明文 JSON 序列化后存储于 localStorage。localStorage 对 XSS 攻击零防护——任何注入页面的恶意脚本均可通过 `localStorage.getItem('uav_auth_token_v1')` 直接读取完整 token。

**影响范围**: 所有认证用户会话。攻击者获得 token 后可完全冒充用户身份。

**修复建议**: 在生产环境中使用 HttpOnly + Secure + SameSite cookie 传输 token，由后端设置，前端无法通过 JS 读取。对于纯 SPA 部署场景，可折中使用 sessionStorage（标签页关闭即清除）配合短期过期策略。`TOKEN_KEY` 常量名 `'uav_auth_token_v1'` 不建议包含版本号，应在迁移时使用不同的键名。

---

### 1.2 演示 Token 无签名验证（严重）

**文件**: `src/stores/auth.js` 第 684 行、第 702 行

```javascript
// 演示模式 token 生成
setToken(`demo.${btoa(username)}.${Date.now()}`)
```

**问题描述**: 演示模式下生成的 token 格式为 `demo.{base64(username)}.{timestamp}`，btoa() 仅是 Base64 编码而非加密，任何人都可以解码出用户名。token 没有任何签名机制，payload 中的 role、exp 等字段由客户端自行构造。

**影响范围**: 演示模式下的所有认证流程。虽然在演示环境中影响有限，但该模式提供了攻击者理解认证机制的入口。

---

### 1.3 JWT Payload 解码无签名验证（严重）

**文件**: `src/stores/auth.js` 第 488-495 行

```javascript
const parts = String(tokenStr).split('.')
if (parts.length >= 2) {
  payload = decodeBase64Url(parts[1])
}
```

**问题描述**: 仅解码 JWT 的 payload 部分（第二个点分隔段），完全不验证签名（第三部分）。即使后端验证了 token 签名，前端盲目信任 payload 内容意味着客户端状态可能与服务端不一致。

---

### 1.4 v-html 渲染用户内容无消毒（高危）

**文件**: `src/views/forum/components/PostDetailModal.vue` 第 39 行

```html
<div class="post-content" v-html="post.content"></div>
```

**问题描述**: `post.content` 通过 `v-html` 直接渲染为 HTML，但整个代码库中未找到任何 HTML 消毒（sanitization）实现。注释中虽标注 "sanitized HTML from trusted source"，实际并未使用 DOMPurify 或任何消毒库。

**修复建议**: 引入 `dompurify` 库，在渲染前对用户内容进行消毒：`import DOMPurify from 'dompurify'; v-html="DOMPurify.sanitize(post.content)"`。同时对后端 API 返回的内容也应进行服务端消毒。

---

### 1.5 innerHTML 用于文本提取可执行脚本（高危）

**文件**: `src/views/forum/ForumView.vue` 第 312-317 行

```javascript
const stripHtml = (html) => {
  const tmp = document.createElement('DIV');
  tmp.innerHTML = html;  // 危险：浏览器会解析并执行脚本
  const text = tmp.textContent || tmp.innerText || '';
  return text.length > 100 ? text.substring(0, 100) + '...' : text;
};
```

**问题描述**: 虽然意图是通过 `textContent` 提取纯文本，但在 `innerHTML` 赋值时浏览器会解析并执行内嵌脚本（如 `<img src=x onerror=alert(1)>`）。`textContent` 的读取发生在脚本执行之后。

**修复建议**: 直接使用 `tmp.textContent = html` 替代 `tmp.innerHTML = html`，这样浏览器不会将内容解析为 HTML，完全避免了 XSS 风险。

---

### 1.6 Token 刷新降级不经服务端验证（高危）

**文件**: `src/stores/auth.js` 第 600-607 行

```javascript
catch (e) {
  console.warn('[AUTH] 生产环境 token 刷新接口未接入，降级为演示续期:', e)
  const newExpiresAt = Date.now() + 3600 * 1000
  tokenExpiresAt.value = newExpiresAt
  startTokenRefreshTimer()
  return { ok: true, mode: 'fallback', expiresAt: newExpiresAt }
}
```

**问题描述**: 当 token 刷新 API 不可用时，代码降级为本地续期——将过期时间无条件延长 1 小时。这意味着即使后端已吊销 token，前端仍认为会话有效。

**修复建议**: 刷新失败时应执行登出操作而非本地续期。添加重试机制最多 3 次，全部失败后清除 token 并跳转到登录页。

---

### 1.7 无内容安全策略（中危）

项目未配置 Content Security Policy (CSP) 头部。缺少 CSP 意味着无法防御 XSS、数据注入等攻击。建议在 Vite 构建配置或 Nginx 层面配置 CSP 头部。

---

## 二、UI/UX 审计

### 2.1 大量硬编码中文文本绕过 i18n（严重）

**文件**: `src/layouts/DefaultLayout.vue`

在 927 行的布局组件中发现 40 余处硬编码的中文字符串，系统切换语言时这些文本不会翻译。关键实例包括：

| 行号 | 硬编码文本 | 应使用 |
|------|-----------|--------|
| 19 | `'欢迎使用演示模式'` | `$t('demo.welcome')` |
| 21 | `您正在使用演示模式体验系统功能...` | `$t('demo.description')` |
| 40, 118 | `WRF 无人机路径规划` | `$t('app.brand')` |
| 80 | `使用文档` | 已存在 `menu.docs` |
| 85 | `设置` | 已存在 `menu.settings` |
| 89 | `系统设置` | `$t('settings.system')` |
| 93 | `主题定制` | `$t('settings.themeCustomizer')` |
| 100 | `权限调试工具` | `$t('settings.permissionDebug')` |
| 286 | `个人信息` | `$t('user.profile')` |
| 292 | `退出登录` | `$t('user.logout')` |
| 353 | `'点击获取位置'` | `$t('location.clickToFetch')` |
| 370 | `['周日','周一',...]` | 使用 `Intl.DateTimeFormat` 或 locale 感知格式 |
| 395 | `'定位中...'` | `$t('location.fetching')` |
| 456 | `'系统已上线'` | `$t('notification.systemOnline')` |
| 580 | `'已退出登录'` | `$t('user.loggedOut')` |
| 194, 203, 255, 266 | `aria-label` 值 | 全部硬编码 |

**修复建议**: 将所有硬编码文本迁移到 `locales/zh-CN.js` 中，使用 `$t()` 引用。对于 `aria-label`，也应在 locale 文件中维护对应的键值。

---

### 2.2 缺失 i18n 键

**文件**: `src/layouts/DefaultLayout.vue` 第 4 行

```html
<a href="#main-content" class="skip-link">{{ $t('app.skipToContent') || '跳到主要内容' }}</a>
```

`app.skipToContent` 键在三个 locale 文件中均不存在，导致回退文本被始终使用。需要在所有 locale 文件中添加此键。

---

### 2.3 无障碍（A11Y）覆盖不完整（中危）

仅有 4 个 `aria-label` 属性分布于整个 927 行的布局组件中。缺失场景包括：

- 侧边栏折叠状态下的图标菜单项无 `aria-label`（Element Plus `el-menu-item` 在折叠模式下只显示图标）
- 用户下拉触发器无 `aria-label`
- 演示模式横幅无角色标注（应使用 `role="alert"`）
- 天气/定位信息区域为纯装饰性交互元素，缺少语义化标记
- 移动端抽屉无 `aria-modal` 属性

---

### 2.4 响应式设计评估（良好，有改进空间）

**优点**:
- 使用 `matchMedia('(max-width: 768px)')` 实现移动端适配
- 移动端使用抽屉替代侧边栏
- `.hide-on-mobile` 类控制在移动端隐藏次要信息
- 两个断点: 768px 和 480px

**改进空间**:
- 缺少平板断点（iPad 竖屏 768-1024px 被当作移动端处理，侧边栏完全隐藏可能过于激进，平板有足够空间显示折叠式侧边栏）
- 320px 极窄屏幕下 header 内容可能溢出（未设置 `flex-wrap`）
- 面包屑标题在 480px 下被截断为 120px，在中文环境下这可能只能容纳 7-8 个字符

---

### 2.5 组件状态覆盖不完整（中危）

检查 `src/components/` 下 9 个共享组件，发现：

- `GaugePanel.vue`、`StatCard.vue` 和 `DataScopeBadge.vue` 在数据为空时没有加载骨架屏或占位符
- `ExcelBatchImporter.vue` 缺少导入失败的错误恢复 UI
- `NetCDFChunkUploader.vue` (22.6KB) 有分块上传进度但缺少网络断开重连后的断点续传指示
- 大多数组件缺少 `v-if/v-else` 模式下的空状态展示

---

## 三、代码质量评估

### 3.1 架构与组件设计（良好）

**优点**:
- 清晰的目录结构：api/、components/、composables/、layouts/、locales/、router/、stores/、utils/、views/
- 使用 Pinia 进行模块化状态管理（auth、app、notification、dataSource、audit 等）
- Composition API 采用 `<script setup>` 语法，代码简洁
- 路由懒加载减轻了初始包体积

**改进空间**:
- `DefaultLayout.vue` 927 行，单一文件承担了布局、导航、时间/天气/位置获取、主题切换等多种职责，建议拆分为多个子组件（HeaderBar、Sidebar、MobileDrawer、LocationWidget、WeatherWidget）
- `auth.js` store 1355 行，权限矩阵和 API 调用逻辑耦合在一起
- 部分视图文件体积较大，如 `ApiConfigView.vue` 约有 1400+ 行

---

### 3.2 TypeScript 使用不充分（低危）

项目支持 TypeScript（存在 `tsconfig.json`），但仅 3 个 composable 文件使用了 `.ts` 扩展名。Store 文件、组件、工具函数均使用纯 JavaScript，缺少类型安全保障。

---

### 3.3 未使用的导入（低危）

`src/views/forum/ForumView.vue` 中存在未使用的导入：
- 第 147 行: `ChatDotRound`、`Collection`（已导入但未使用）
- 第 175 行: `canReply`（声明但未引用）

---

### 3.4 CSS 组织方式不一致（低危）

项目中同时存在多种样式方法：
- `scoped` 样式（大多数组件）
- 内联样式（`style="... "` 在模板中多处出现）
- `:deep()` 穿透样式
- 全局样式

建议统一使用 `scoped` 配合 CSS 变量，避免内联样式污染模板可读性。

---

## 四、性能分析

### 4.1 构建优化（良好）

- Vite 作为构建工具，开发服务器启动快，HMR 即时
- 路由已配置懒加载（动态 import）
- Element Plus 组件按需导入（通过 `unplugin-element-plus`）

### 4.2 运行时性能（有改进空间）

**高优先级**: `src/layouts/DefaultLayout.vue` 第 467 行

```javascript
timeInterval = setInterval(updateTime, 1000)  // 每秒更新一次时钟
```

`setInterval` 每秒触发一次 `updateTime()`，该函数执行 `new Date()` 和字符串拼接，并导致 Vue 响应式更新。虽然单次开销小，但持续运行是一个不必要的持续渲染源。建议将间隔增加到 30 秒，并移除秒数显示以降低用户对精确时间的期望。

**中优先级**: WebSocket 连接在 `useWebSocket.ts` 和 `websocket.js` 中均存在。`composables/useWebSocket.ts` 和 `plugins/websocket.ts` 似乎是两个独立实现，可能造成重复连接。建议统一使用一个 WebSocket 管理方案。

**中优先级**: `NetCDFChunkUploader.vue` 中存在 `crypto.subtle.digest()` 调用用于计算 SHA-256 哈希，这在主线程执行可能阻塞 UI。建议将哈希计算移至 Web Worker。

---

### 4.3 资源加载

- 图片资源均使用动态绑定，已添加 `loading="lazy"` 和 `width`/`height` 属性（已修复）
- 未发现未压缩的大图片资源
- 缺失对关键 CSS 的内联处理（Critical CSS inline）

### 4.4 内存管理（良好）

- `onBeforeUnmount` 中正确清理了 `setInterval` 定时器
- WebSocket 连接在组件卸载时断开
- `matchMedia` 监听器正确移除

---

## 五、功能验证

### 5.1 路由与导航（良好）

- 路由基于角色权限过滤菜单项（`authStore.hasRouteAccess(item.key)`）
- 路由守卫 `beforeEach` 检查认证状态（`src/router/index.js`）
- 默认路由根据角色动态计算（`appStore.getDefaultRoute(authStore.role)`）
- 所有主路由均配置了 `meta.title`

### 5.2 表单验证（有改进空间）

**登录表单**: 用户名和密码仅做了 `required` 校验。生产环境中应增加：用户名长度限制、密码复杂度校验、防止暴力破解的速率限制。

**注册表单**: 邮箱验证正则表达式为 `/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,63}$/`，允许顶级域名为纯字母（不包含数字 TLD），且未验证邮箱域名是否存在。

### 5.3 错误处理（需改进）

- 多数 API 调用使用 `try/catch`，但错误仅通过 `ElMessage.error()` 显示，缺少结构化错误分类
- 网络错误、认证错误、业务逻辑错误未区分处理
- 缺少全局错误边界组件（error boundary）

### 5.4 Mock 与演示模式（良好）

- Mock 拦截器通过 axios interceptor 实现，仅在 `import.meta.env.DEV` 下启用
- 演示模式有明确的视觉标识（顶部横幅、模式标签）
- Mock 数据路径统一使用 `/mock-api/` 前缀，便于切换真实 API

---

## 六、跨浏览器兼容性

- 使用了 `matchMedia` 标准 API，兼容 IE10+
- 使用了 CSS Grid（`.login-container`、`.register-container`），兼容所有现代浏览器
- `crypto.subtle` API 需要 HTTPS 或 localhost 环境
- `ResizeObserver` 未在代码中发现使用，无需 polyfill
- 未发现使用需要 vendor prefix 的 CSS 属性

---

## 问题优先级总结

| 优先级 | 数量 | 关键问题 |
|--------|------|---------|
| 严重 | 3 | Token localStorage 存储、Token 无签名验证、v-html 无消毒 |
| 高危 | 3 | innerHTML 脚本执行、Token 刷新降级、40+ 硬编码文本 |
| 中危 | 9 | 缺失 i18n 键、A11Y 覆盖不足、组件状态覆盖、CSP 缺失、WebSocket 重复实现、表单验证弱、时钟性能、NetCDF 哈希主线程阻塞、平板断点缺失 |
| 低危 | 7 | TypeScript 使用不足、未使用导入、CSS 组织不一致、大文件未拆分、缺少错误边界、缺失 Critical CSS、内联样式 |

---

## 附录 A: 修改文件清单

在本次审计之前已执行的安全/无障碍修复：

1. `src/layouts/DefaultLayout.vue` - aria-label、skip link、prefers-reduced-motion、color-scheme、重复 CSS 修复、时钟节流
2. `src/views/auth/LoginView.vue` - emoji 图标替换、autocomplete、aria-busy、演示凭据折叠
3. `src/views/auth/RegisterView.vue` - emoji 图标替换、autocomplete、邮箱正则增强
4. `src/views/orders/OrderView.vue` - 移除 console.log 调试日志
5. `src/views/config/ApiConfigView.vue` - 移除 console.log 调试日志
6. `src/mock/index.js` - 移除 window.__MOCK_ENABLED__ 全局变量
7. `src/views/forum/ForumView.vue` - 图片 width/height/loading=lazy
8. `src/views/forum/components/PostDetailModal.vue` - 图片 width/height/loading=lazy
9. `README.md` - 移除 window.__MOCK_ENABLED__ 引用

## 附录 B: 建议修复路线图

**第 1 阶段（紧急 - 1-3 天）**: 修复 3 个严重安全问题（token 存储、v-html 消毒、innerHTML 修复）

**第 2 阶段（重要 - 1 周）**: 完成所有硬编码文本的 i18n 迁移，补全 locale 文件中缺失的键

**第 3 阶段（标准 - 1-2 周）**: 增强 A11Y 覆盖、添加 CSP 头部、完善组件 loading/error/empty 状态、修复 token 刷新降级逻辑

**第 4 阶段（优化 - 持续）**: 拆分大文件、逐步迁移到 TypeScript、统一 CSS 方案、移除未使用导入、WebSocket 方案统一、NetCDF 哈希 Web Worker 化
