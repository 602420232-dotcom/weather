import { defineStore } from 'pinia'
import { ref, computed, watch, Ref, ComputedRef } from 'vue'
import { ROLES } from './auth'
import { isNightTime as computeIsNightTime, nextTransitionSeconds } from '../composables/useSunTime'
import i18n from '../locales'

const APP_STORAGE_KEY = 'uav_app_config'
const THEME_STORAGE_KEY = 'uav_theme_v1'
const THEME_MODE_STORAGE_KEY = 'uav_theme_mode_v1'

type ThemeMode = 'light' | 'dark' | 'brand' | 'highContrast' | 'auto'

const DARK_THEMES: ThemeMode[] = ['dark', 'brand', 'highContrast']
const LIGHT_THEMES: ThemeMode[] = ['light']

// 不同角色的默认首页
const DEFAULT_HOME_BY_ROLE: Record<string, string> = {
  [ROLES.USER]: 'dashboard',
  [ROLES.PRODUCTION]: 'dashboard',
  [ROLES.FLIGHT]: 'dashboard',
  [ROLES.TESTER]: 'dashboard',
  [ROLES.DEPLOYMENT]: 'monitoring',
  [ROLES.ADMIN]: 'dashboard'
}

function applyDataThemeToDocument(val: string): void {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', val)
  const isDarkMode = DARK_THEMES.includes(val as ThemeMode)
  document.documentElement.style.setProperty('--color-blue-filter', isDarkMode ? '0.85' : '1')
  // Element Plus 暗色主题支持 - 使用 class 方式
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
}

// 检测系统主题
function getSystemTheme(): ThemeMode {
  try {
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark'
    }
  } catch (_) {}
  return 'light'
}

export const useAppStore = defineStore('app', () => {
  const collapsed: Ref<boolean> = ref(true)
  const sidebarManual: Ref<boolean> = ref(false)
  const theme: Ref<string> = ref(getSystemTheme()) // 默认使用系统主题
  const themeMode: Ref<ThemeMode> = ref('auto')
  const language: Ref<string> = ref('zh')
  const defaultRoute: Ref<string> = ref('')
  const envMode: Ref<string> = ref('demo')
  const isNight: Ref<boolean> = ref(false)
  let autoThemeTimer: ReturnType<typeof setTimeout> | null = null

  const effectiveTheme: ComputedRef<ThemeMode> = computed(() => {
    if (themeMode.value === 'auto') {
      return isNight.value ? 'dark' : 'light'
    }
    return themeMode.value
  })

  const isDark: ComputedRef<boolean> = computed(() => DARK_THEMES.includes(effectiveTheme.value))

  function applyCurrentTheme(): void {
    theme.value = effectiveTheme.value
    applyDataThemeToDocument(theme.value)
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme.value)
    } catch (_) {}
  }

  function scheduleAutoThemeCheck(): void {
    if (autoThemeTimer) {
      clearTimeout(autoThemeTimer)
      autoThemeTimer = null
    }
    if (typeof window === 'undefined') return
    const seconds = Math.min(nextTransitionSeconds(), 3600)
    autoThemeTimer = setTimeout(() => {
      isNight.value = computeIsNightTime()
      if (themeMode.value === 'auto') {
        applyCurrentTheme()
      }
      scheduleAutoThemeCheck()
    }, seconds * 1000)
  }

  function init(): void {
    // 从 localStorage 读取配置（带错误处理，防止被浏览器阻止）
    try {
      const raw = localStorage.getItem(APP_STORAGE_KEY)
      if (raw) {
        const saved = JSON.parse(raw)
        if (saved.collapsed !== undefined) collapsed.value = saved.collapsed
        if (saved.sidebarManual !== undefined) sidebarManual.value = saved.sidebarManual
        if (saved.theme) theme.value = saved.theme
        if (saved.language) language.value = saved.language
        if (saved.defaultRoute !== undefined) defaultRoute.value = saved.defaultRoute
        if (saved.envMode) envMode.value = saved.envMode
      }
    } catch (e) {
      // localStorage 被阻止，使用默认值
    }
    // theme / themeMode 独立 key 优先
    try {
      const themeRaw = localStorage.getItem(THEME_STORAGE_KEY)
      if (themeRaw) theme.value = themeRaw
    } catch (_) {}
    try {
      const modeRaw = localStorage.getItem(THEME_MODE_STORAGE_KEY)
      if (modeRaw) themeMode.value = modeRaw as ThemeMode
    } catch (_) {}

    isNight.value = computeIsNightTime()
    // 对于 auto 模式，用日夜判断覆盖 theme
    if (themeMode.value === 'auto') {
      theme.value = isNight.value ? 'dark' : 'light'
    }
    // 始终应用当前主题到 document
    applyDataThemeToDocument(theme.value)

    // 读取独立的 envMode key
    try {
      const envRaw = localStorage.getItem('uav_app_env_mode_v1')
      if (envRaw) envMode.value = envRaw
    } catch (_) {}

    scheduleAutoThemeCheck()
  }

  function persist(): void {
    try {
      localStorage.setItem(
        APP_STORAGE_KEY,
        JSON.stringify({
          collapsed: collapsed.value,
          theme: theme.value,
          themeMode: themeMode.value,
          language: language.value,
          defaultRoute: defaultRoute.value,
          envMode: envMode.value
        })
      )
    } catch (e) {}
    try {
      localStorage.setItem(THEME_MODE_STORAGE_KEY, String(themeMode.value))
    } catch (e) {}
  }

  function setEnvMode(val: string): void {
    envMode.value = val
    persist()
    try {
      localStorage.setItem('uav_app_env_mode_v1', String(val))
    } catch (e) {}
  }

  function toggleSidebar(): void {
    collapsed.value = !collapsed.value
    persist()
  }

  function setCollapsed(val: boolean): void {
    collapsed.value = val
    persist()
  }

  function toggleTheme(): void {
    if (themeMode.value === 'auto') {
      themeMode.value = 'light'
    } else {
      const order: ThemeMode[] = ['light', 'dark', 'brand', 'highContrast', 'auto']
      const idx = order.indexOf(themeMode.value)
      themeMode.value = order[(idx + 1) % order.length]
    }
    isNight.value = computeIsNightTime()
    applyCurrentTheme()
    persist()
  }

  function setTheme(val: string): void {
    const valid: ThemeMode[] = ['light', 'dark', 'brand', 'highContrast', 'auto']
    themeMode.value = valid.includes(val as ThemeMode) ? (val as ThemeMode) : 'light'
    isNight.value = computeIsNightTime()
    applyCurrentTheme()
    persist()
  }

  function setThemeMode(val: string): void {
    const valid: ThemeMode[] = ['light', 'dark', 'brand', 'highContrast', 'auto']
    themeMode.value = valid.includes(val as ThemeMode) ? (val as ThemeMode) : 'auto'
    isNight.value = computeIsNightTime()
    applyCurrentTheme()
    persist()
  }

  function refreshNightStatus(): void {
    isNight.value = computeIsNightTime()
    if (themeMode.value === 'auto') applyCurrentTheme()
  }

  function setLanguage(val: string): void {
    language.value = val
    const localeMap: Record<string, string> = { 'zh': 'zh-CN', 'en': 'en-US', 'ja': 'ja-JP' }
    const locale = localeMap[val] || val
    i18n.global.locale.value = locale
    localStorage.setItem('locale', locale)
    persist()
  }

  function setDefaultRoute(routeKey: string): void {
    defaultRoute.value = routeKey
    persist()
  }

  function getDefaultRoute(role: string): string {
    if (defaultRoute.value) return defaultRoute.value
    return DEFAULT_HOME_BY_ROLE[role] || 'dashboard'
  }

  init()

  // 主题模式变化时自动持久化 + 应用
  watch(themeMode, () => {
    isNight.value = computeIsNightTime()
    applyCurrentTheme()
    persist()
  })

  return {
    collapsed,
    theme,
    themeMode,
    effectiveTheme,
    language,
    defaultRoute,
    envMode,
    isDark,
    isNight,
    toggleSidebar,
    setCollapsed,
    toggleTheme,
    setTheme,
    setThemeMode,
    setLanguage,
    setDefaultRoute,
    setEnvMode,
    getDefaultRoute,
    refreshNightStatus,
    init
  }
})
