/**
 * Token 管理 Store
 * 负责 JWT Token 的生成、存储、持久化、刷新和生命周期管理
 */
import { defineStore } from 'pinia'
import { ref, computed, Ref, ComputedRef } from 'vue'

const TOKEN_KEY = 'uav_auth_token_v1'

export interface TokenPayload {
  sub: string
  role: string
  demo?: boolean
  iat: number
  exp: number
}

export interface StoredToken {
  token: string
  payload: TokenPayload
  expiresAt: number
}

export interface RefreshResult {
  ok: boolean
  mode: 'demo' | 'prod' | 'failed'
  expiresAt?: number
  reason?: string
}

export interface SetTokenOptions {
  demoMode?: boolean
  username?: string
  role?: string
}

export const useTokenManager = defineStore('tokenManager', () => {
  const token: Ref<string | null> = ref(null)
  const payload: Ref<TokenPayload | null> = ref(null)
  const expiresAt: Ref<number> = ref(0)
  let refreshTimer: ReturnType<typeof setTimeout> | null = null

  const isExpired: ComputedRef<boolean> = computed(() => {
    if (!expiresAt.value) return true
    return Date.now() >= expiresAt.value
  })

  const remainingSeconds: ComputedRef<number> = computed(() => {
    if (!expiresAt.value) return 0
    return Math.max(0, Math.floor((expiresAt.value - Date.now()) / 1000))
  })

  /** Base64Url 解码 */
  function decodeBase64Url(str: string): TokenPayload | null {
    try {
      const base64 = String(str).replace(/-/g, '+').replace(/_/g, '/')
      const decoded = typeof atob === 'function'
        ? atob(base64)
        : (window as any).Buffer?.from(base64, 'base64').toString('utf-8')
      if (!decoded) return null
      return JSON.parse(decoded) as TokenPayload
    } catch (e) {
      return null
    }
  }

  /** 设置 Token */
  function setToken(tokenStr: string | null, opts: SetTokenOptions = {}): TokenPayload | null {
    const { demoMode = false, username = 'demo', role = 'user' } = opts

    if (!tokenStr) {
      token.value = null
      payload.value = null
      expiresAt.value = 0
      clearPersisted()
      stopRefreshTimer()
      return null
    }

    let p: TokenPayload | null = null
    let exp: number = 0

    if (demoMode) {
      exp = Date.now() + 3600 * 1000
      p = {
        sub: username,
        role,
        demo: true,
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(exp / 1000)
      }
    } else {
      try {
        const parts = String(tokenStr).split('.')
        if (parts.length >= 2) p = decodeBase64Url(parts[1])
      } catch (e) {
        console.warn('[TokenManager] JWT 解析失败:', e)
      }
      exp = (p && p.exp) ? p.exp * 1000 : Date.now() + 3600 * 1000
    }

    token.value = tokenStr
    payload.value = p
    expiresAt.value = exp
    persistToken(tokenStr, p, exp)
    startRefreshTimer()

    return p
  }

  /** 检查 Token 是否已过期（可设阈值秒） */
  function isTokenExpired(thresholdSec: number = 60): boolean {
    if (!expiresAt.value) return true
    return Date.now() + (thresholdSec * 1000) > expiresAt.value
  }

  /** 持久化到 localStorage */
  function persistToken(tokenStr: string, p: TokenPayload | null, exp: number): void {
    try {
      localStorage.setItem(TOKEN_KEY, JSON.stringify({ token: tokenStr, payload: p, expiresAt: exp }))
    } catch (e) {
      console.warn('[TokenManager] 持久化失败:', e)
    }
  }

  /** 清除持久化的 Token */
  function clearPersisted(): void {
    try { localStorage.removeItem(TOKEN_KEY) } catch (e) {}
  }

  /** 从 localStorage 恢复 Token */
  function restoreToken(): boolean {
    if (token.value) return true
    try {
      const raw = localStorage.getItem(TOKEN_KEY)
      if (raw) {
        const parsed: StoredToken = JSON.parse(raw)
        token.value = parsed.token || null
        payload.value = parsed.payload || null
        expiresAt.value = parsed.expiresAt || 0
        if (!isTokenExpired()) startRefreshTimer()
        return true
      }
    } catch (e) {
      clearPersisted()
    }
    return false
  }

  /** 清除 Token */
  function clearToken(): void {
    stopRefreshTimer()
    token.value = null
    payload.value = null
    expiresAt.value = 0
    clearPersisted()
  }

  /**
   * 刷新 Token
   */
  async function doRefresh(
    doDemoRefresh?: () => Promise<{ expiresAt: number }>,
    isDemoMode?: boolean
  ): Promise<RefreshResult> {
    if (isDemoMode && doDemoRefresh) {
      return doDemoRefresh() as unknown as RefreshResult
    }

    try {
      const { default: api } = await import('../api')
      const res = await api.post('/auth/refresh')
      if (res && res.token) {
        setToken(res.token)
        return { ok: true, mode: 'prod' }
      }
      throw new Error('刷新接口未返回 token')
    } catch (e) {
      console.warn('[TokenManager] 生产环境 token 刷新失败:', e)
      const graceExpiresAt = Date.now() + 120 * 1000
      expiresAt.value = graceExpiresAt
      startRefreshTimer()
      return { ok: false, mode: 'failed', reason: 'REFRESH_API_UNAVAILABLE', expiresAt: graceExpiresAt }
    }
  }

  /** 启动 Token 刷新定时器 */
  function startRefreshTimer(): void {
    stopRefreshTimer()
    const refreshBeforeSec = 300
    let nextRefreshMs = Math.max(0, expiresAt.value - Date.now() - refreshBeforeSec * 1000)
    if (nextRefreshMs < 1000) nextRefreshMs = 300000

    refreshTimer = setTimeout(() => {
      console.info('[TokenManager] 到期刷新提醒')
    }, nextRefreshMs)
  }

  /** 停止 Token 刷新定时器 */
  function stopRefreshTimer(): void {
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  return {
    token,
    payload,
    expiresAt,
    isExpired,
    remainingSeconds,
    setToken,
    isTokenExpired,
    doRefresh,
    restoreToken,
    clearToken,
    clearPersisted,
    startRefreshTimer,
    stopRefreshTimer
  }
})
