// ===== Monkey-patch addEventListener: scroll-blocking events default to passive =====
// Must run BEFORE any library that registers wheel/mousewheel listeners (e.g. ECharts/ZRender)
// Otherwise Chrome throws [Violation] warnings on every chart init
;(function () {
  var _add = EventTarget.prototype.addEventListener
  var passiveEvents = ['mousewheel', 'wheel', 'touchstart', 'touchmove', 'scroll']
  EventTarget.prototype.addEventListener = function (type, listener, options) {
    var opts = options
    if (passiveEvents.indexOf(type) !== -1) {
      if (opts === undefined || opts === null) {
        opts = { passive: true }
      } else if (typeof opts === 'boolean') {
        opts = { capture: opts, passive: true }
      } else if (typeof opts === 'object' && opts.passive === undefined) {
        opts = { capture: opts.capture, passive: true, once: opts.once, signal: opts.signal }
      }
    }
    return _add.call(this, type, listener, opts)
  }
})()

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './styles/theme-vars.css'
import './styles/variables.css'
import './styles/breakpoints.css'
import './styles/index.css'
import './styles/theme-dark.css'

// 主题初始化：优先使用 localStorage，否则检测系统主题
let savedTheme = null
try {
  savedTheme = localStorage.getItem('uav_theme_v1')
} catch (e) {
  // localStorage 被浏览器阻止，使用系统主题
}

let theme = 'light'
if (savedTheme) {
  theme = savedTheme
} else {
  // 自动检测系统主题
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  theme = prefersDark ? 'dark' : 'light'
  try {
    localStorage.setItem('uav_theme_v1', theme)
  } catch (e) {
    // localStorage 被阻止，忽略
  }
}

// 应用主题到 document
document.documentElement.setAttribute('data-theme', theme)
const isDarkMode = theme === 'dark' || theme === 'brand' || theme === 'highContrast'
if (isDarkMode) {
  document.documentElement.classList.add('dark')
  // 设置深色模式背景变量
  document.documentElement.style.setProperty('--bg-primary', '#0a0e1a')
  document.documentElement.style.setProperty('--bg-secondary', '#111827')
  document.documentElement.style.setProperty('--bg-tertiary', '#1a2234')
  document.documentElement.style.setProperty('--text-primary', '#c9d1d9')
  document.documentElement.style.setProperty('--text-secondary', '#8b949e')
  document.documentElement.style.setProperty('--border-color', 'rgba(255, 255, 255, 0.08)')
} else {
  document.documentElement.classList.remove('dark')
  // 设置浅色模式背景变量
  document.documentElement.style.setProperty('--bg-primary', '#f0f2f5')
  document.documentElement.style.setProperty('--bg-secondary', '#ffffff')
  document.documentElement.style.setProperty('--bg-tertiary', '#f5f7fa')
  document.documentElement.style.setProperty('--text-primary', 'rgba(0, 0, 0, 0.88)')
  document.documentElement.style.setProperty('--text-secondary', 'rgba(0, 0, 0, 0.65)')
  document.documentElement.style.setProperty('--border-color', '#dcdfe6')
}

import { setElMessage, handleGenericError } from './utils/errorHandler'
import i18n from './locales'
import { setupMock } from './mock'
import idb from './utils/indexedDB'

// 仅在 DEV 环境启用前端 Mock 服务（拦截 /mock-api/* 返回本地 JSON）
// 生产环境 setupMock 内部直接短路返回，不会注册任何拦截器
if (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.DEV) {
  setupMock()
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(i18n)

// 自定义主题变量恢复：在 Pinia 初始化之后执行，避免 app.init() 的 applyDataThemeToDocument 覆盖
// applyCustom() 保存的主题键是 'dark'/'light' 而非 'custom'，所以改用 uav_theme_custom_active 判断
try {
  const isCustomActive = localStorage.getItem('uav_theme_custom_active') === 'true'
  if (isCustomActive) {
    const raw = localStorage.getItem('uav_theme_custom_vars_v1')
    if (raw) {
      const vars = JSON.parse(raw)
      Object.entries(vars).forEach(([k, v]) => {
        document.documentElement.style.setProperty(k, v)
      })
      document.documentElement.setAttribute('data-theme-custom', 'true')
    }
  }
} catch (_) {}

// 从 Element Plus 取到 ElMessage 后注入到 errorHandler，
// 使 API 拦截器与全局错误处理器共用同一份提示组件
import { ElMessage } from 'element-plus'
setElMessage(ElMessage)

// ===== 全局 Vue 错误处理：捕获组件渲染 / 生命周期 / 事件处理异常 =====
app.config.errorHandler = (err, instance, info) => {
  const componentName =
    (instance && instance.$options && instance.$options.name) ||
    (instance && instance.type && instance.type.name) ||
    'UnknownComponent'
  console.error('[Global Error]', {
    message: err && err.message,
    component: componentName,
    info
  }, err)
  handleGenericError(err, { context: `vue:${componentName}:${info || 'render'}` })
}

// ===== 全局未捕获 Promise 错误（浏览器级）=====
if (typeof window !== 'undefined') {
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event && (event.reason || event.detail && event.detail.reason)
    console.warn('[UnhandledRejection]', reason)
    handleGenericError(reason, { context: 'unhandled-promise' })
  })
  window.addEventListener('error', (event) => {
    // 资源加载错误等
    const err = event && event.error
    if (err) {
      console.error('[Window Error]', err)
      handleGenericError(err, { context: 'window-error' })
    }
  })
}

app.mount('#app')

// 全局 t() 函数，方便在浏览器控制台调试（i18n）
window.$t = (key, params) => {
  try { return i18n.global.t(key, params) } catch (e) { return key }
}
window.setLocale = (locale) => {
  i18n.global.locale.value = locale
  localStorage.setItem('locale', locale)
  console.info('[i18n] 语言切换为：', locale)
}

// ============ IndexedDB 初始化（离线缓存底座） ============
if (idb && idb.isAvailable && typeof idb.initDB === 'function') {
  idb.initDB()
    .then(() => {
      if (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.DEV) {
        console.info('[main] IndexedDB 已初始化')
      }
      if (idb.getStorageInfo) {
        return idb.getStorageInfo().then((info) => {
          if (info && typeof info.quota === 'number') {
            console.info('[main] 存储配额', {
              quota: (info.quota / 1024 / 1024).toFixed(2) + ' MB',
              usage: (info.usage / 1024 / 1024).toFixed(2) + ' MB',
              usagePercent: info.usagePercent + '%'
            })
          }
        })
      }
    })
    .catch((err) => {
      console.warn('[main] IndexedDB 不可用，降级为纯内存 + localStorage 缓存', err && err.message)
    })
}

// ============ Service Worker 注册（PWA 离线能力） ============
// 双策略：
//   A. 如已安装 vite-plugin-pwa，会在 dist 生成 /sw.js，优先注册
//   B. 若未安装，则注册 /service-worker.js（手写版，提供基础离线能力）
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    const preferredSW = import.meta.env.DEV
      ? '/service-worker.js'
      : '/sw.js'

    const tryRegister = (url, fallbackUrl) => {
      navigator.serviceWorker
        .register(url)
        .then((reg) => {
          if (reg.installing) {
            console.info('[PWA] 新 Service Worker 安装中：', url)
          } else if (reg.waiting) {
            console.info('[PWA] 有新版本待激活：', url)
          } else if (reg.active) {
            console.info('[PWA] Service Worker 已激活：', url)
          }
        })
        .catch((err) => {
          console.warn('[PWA] 注册失败，尝试降级方案：', url, err)
          if (fallbackUrl && fallbackUrl !== url) {
            navigator.serviceWorker.register(fallbackUrl).catch((e) => {
              console.warn('[PWA] 降级方案也失败（不影响主功能）：', e)
            })
          }
        })
    }

    tryRegister(preferredSW, '/service-worker.js')
  })

  // 网络状态变化：友好提示（不阻塞 UI）
  let offlineToastShown = false
  window.addEventListener('online', () => {
    offlineToastShown = false
    try {
      const ElMessage = ElementPlus && ElementPlus.ElMessage
      if (typeof ElMessage === 'function') ElMessage({ type: 'success', message: '网络已恢复', duration: 2000 })
      else console.info('[PWA] 网络已恢复')
    } catch (_) {}
  })
  window.addEventListener('offline', () => {
    if (offlineToastShown) return
    offlineToastShown = true
    try {
      const ElMessage = ElementPlus && ElementPlus.ElMessage
      if (typeof ElMessage === 'function') ElMessage({ type: 'warning', message: '已离线 - 使用本地缓存数据', duration: 3000 })
      else console.warn('[PWA] 已离线，使用本地缓存数据')
    } catch (_) {}
  })
}
