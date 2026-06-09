import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { logAction, AUDIT_ACTIONS } from '../utils/audit'
import { useTokenManager } from './tokenManager'
import { usePermissionStore } from './permissions'
import {
  ROLES, ROLE_LABELS, ROLE_LABELS_EN,
  TEAMS, TEAM_LABELS, DATA_SCOPES, DATA_SCOPE_LABELS,
  DEFAULT_DATA_SCOPE_BY_ROLE, DEMO_USER_TEAM_MAP,
  DEFAULT_ACCOUNTS, DEMO_USER_MAP
} from './constants/roles'

// ===== 重新导出常量（向后兼容） =====
export {
  ROLES, ROLE_LABELS, ROLE_LABELS_EN,
  TEAMS, TEAM_LABELS, DATA_SCOPES, DATA_SCOPE_LABELS,
  DEFAULT_DATA_SCOPE_BY_ROLE, DEMO_USER_TEAM_MAP,
  DEFAULT_ACCOUNTS, DEMO_USER_MAP
} from './constants/roles'

export { PERMISSION_MATRIX, ACTION_PERMISSIONS } from './constants/permissionsMatrix'

// ===== 存储 Key =====
const STORAGE_KEY = 'uav_auth_user'
const DEMO_MODE_KEY = 'uav_demo_mode'
const DEMO_SHOWN_KEY = 'uav_demo_shown'
const TOKEN_KEY = 'uav_auth_token_v1'

// ===== Pinia Store =====
export const useAuthStore = defineStore('auth', () => {
  const tokenManager = useTokenManager()
  const permissionStore = usePermissionStore()

  const user = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const demoMode = ref(true)
  const demoShownOnce = ref(false)

  // === 计算属性（用户） ===
  const isLoggedIn = computed(() => !!user.value)
  const role = computed(() => user.value?.role || null)
  const roleLabel = computed(() => ROLE_LABELS[role.value] || '')
  const roleLabelEn = computed(() => ROLE_LABELS_EN[role.value] || '')
  const displayName = computed(() => user.value?.displayName || user.value?.username || '')
  const username = computed(() => user.value?.username || '')
  const userId = computed(() => user.value?.id || user.value?.username || null)
  const team = computed(() => user.value?.team || '')
  const teamLabel = computed(() => TEAM_LABELS[team.value] || team.value || '')
  const dataScope = computed(() => user.value?.dataScope || (DEFAULT_DATA_SCOPE_BY_ROLE[role.value] || 'personal'))
  const dataScopeLabel = computed(() => DATA_SCOPE_LABELS[dataScope.value] || '')

  const accessibleRoutes = computed(() => {
    if (!role.value) return []
    return permissionStore.getAccessibleRoutes(role.value)
  })
  const tokenRemainingSeconds = computed(() => tokenManager.remainingSeconds)

  // === 权限方法（委托给 permissionStore） ===
  function hasRouteAccess(routeKey) {
    return permissionStore.hasRouteAccess(role.value, routeKey)
  }
  function hasAction(actionKey) {
    return permissionStore.hasAction(role.value, actionKey)
  }
  function canSee(owner, itemTeam) {
    if (dataScope.value === 'all') return true
    if (dataScope.value === 'personal') return owner && userId.value && String(owner) === String(userId.value)
    if (dataScope.value === 'team') return itemTeam && team.value && itemTeam === team.value
    return true
  }
  function getVisibleOwnerIds() {
    if (dataScope.value === 'all') return null
    if (dataScope.value === 'personal') return userId.value ? [userId.value] : []
    if (dataScope.value === 'team') return [team.value]
    return null
  }

  // === 用户持久化 ===
  function setUser(userData) {
    user.value = userData
    if (userData) {
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(userData)) } catch (e) {}
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  function initFromStorage() {
    if (user.value) return
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) user.value = JSON.parse(raw)
      const demoModeRaw = localStorage.getItem(DEMO_MODE_KEY)
      if (demoModeRaw !== null) demoMode.value = demoModeRaw === 'true'
      const shownRaw = localStorage.getItem(DEMO_SHOWN_KEY)
      if (shownRaw !== null) demoShownOnce.value = shownRaw === 'true'
    } catch (e) {
      console.warn('Failed to parse user from localStorage', e)
      localStorage.removeItem(STORAGE_KEY)
    }
    tokenManager.restoreToken()
    permissionStore.restoreNacosMatrix()
    if (tokenManager.token && !tokenManager.isTokenExpired()) {
      startTokenRefreshTimer()
    }
  }

  function setDemoMode(isDemo) {
    demoMode.value = isDemo
    try { localStorage.setItem(DEMO_MODE_KEY, String(isDemo)) } catch (e) {}
  }

  function markDemoShown() {
    demoShownOnce.value = true
    try { localStorage.setItem(DEMO_SHOWN_KEY, 'true') } catch (e) {}
  }

  // === 团队与数据范围 ===
  function updateTeam(newTeam) {
    if (!TEAMS.includes(newTeam)) return false
    if (!user.value) return false
    const oldTeam = user.value.team || ''
    user.value.team = newTeam
    setUser(user.value)
    try {
      logAction({
        action: AUDIT_ACTIONS.CHANGE_TEAM,
        target: newTeam,
        detail: `${oldTeam || '-'} → ${newTeam}`,
        level: 'info'
      })
    } catch (e) {}
    return true
  }

  function updateDataScope(newScope) {
    if (!DATA_SCOPES.includes(newScope)) return false
    if (!user.value) return false
    const oldScope = user.value.dataScope || ''
    user.value.dataScope = newScope
    setUser(user.value)
    try {
      logAction({
        action: AUDIT_ACTIONS.CHANGE_DATA_SCOPE,
        target: newScope,
        detail: `${oldScope || '-'} → ${newScope}`,
        level: 'info'
      })
    } catch (e) {}
    return true
  }

  // === Token 响应式代理（computed 保证对 tokenManager 变更的响应） ===
  const token = computed(() => tokenManager.token)
  const tokenPayload = computed(() => tokenManager.payload)
  const tokenExpiresAt = computed(() => tokenManager.expiresAt)

  // === Token 方法（委托给 tokenManager） ===
  function setToken(tokenStr) {
    return tokenManager.setToken(tokenStr, {
      demoMode: demoMode.value,
      username: user.value?.username || 'demo',
      role: user.value?.role || 'user'
    })
  }
  function isTokenExpired(thresholdSec = 60) {
    return tokenManager.isTokenExpired(thresholdSec)
  }

  // === Token 刷新 ===
  let refreshIntervalId = null

  function startTokenRefreshTimer() {
    stopTokenRefreshTimer()
    tokenManager.startRefreshTimer()
    const refreshBeforeSec = 300
    let nextRefreshMs = Math.max(0, tokenManager.expiresAt - Date.now() - refreshBeforeSec * 1000)
    if (nextRefreshMs < 1000) {
      refreshToken().catch(e => console.error('[AUTH] 立即刷新失败:', e))
      nextRefreshMs = 300000
    }
    refreshIntervalId = setTimeout(async () => {
      try {
        await refreshToken()
      } catch (e) {
        console.error('[AUTH] 定时刷新失败:', e)
        if (isLoggedIn.value) {
          refreshIntervalId = setTimeout(() => startTokenRefreshTimer(), 30000)
        }
      }
    }, nextRefreshMs)
  }

  function stopTokenRefreshTimer() {
    if (refreshIntervalId) {
      clearTimeout(refreshIntervalId)
      refreshIntervalId = null
    }
    tokenManager.stopRefreshTimer()
  }

  async function refreshToken() {
    try {
      if (demoMode.value) {
        const newExpiresAt = Date.now() + 3600 * 1000
        tokenManager.expiresAt = newExpiresAt
        if (tokenManager.payload) {
          tokenManager.payload = { ...tokenManager.payload, exp: Math.floor(newExpiresAt / 1000) }
        }
        try {
          const stored = JSON.parse(localStorage.getItem(TOKEN_KEY) || '{}')
          stored.expiresAt = newExpiresAt
          if (stored.payload) stored.payload.exp = Math.floor(newExpiresAt / 1000)
          localStorage.setItem(TOKEN_KEY, JSON.stringify(stored))
        } catch (e) {}
        startTokenRefreshTimer()
        return { ok: true, mode: 'demo', expiresAt: newExpiresAt }
      }
      return await tokenManager.doRefresh(null, false)
    } catch (e) {
      console.error('[AUTH] token 刷新失败:', e)
      throw e
    }
  }

  // === 二次确认 ===
  async function requireSensitiveConfirmation(actionLabel) {
    try {
      const label = actionLabel || '该操作'
      const passwordPrompt = demoMode.value
        ? '（演示模式：输入任意字符即可通过）'
        : '（请输入登录密码）'
      const message = `即将执行敏感操作：${label}\n\n请确认已了解操作后果${passwordPrompt}`
      await ElMessageBox.prompt(message, '敏感操作二次确认', {
        confirmButtonText: '确认执行',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        inputType: 'password',
        inputPlaceholder: '请输入密码以确认',
        showCancelButton: true,
        distinguishCancelAndClose: true,
        beforeClose: (action, instance, done) => {
          if (action !== 'confirm') { done(); return }
          const val = String(instance.inputValue || '').trim()
          if (!val) { ElMessage.warning('请输入密码以确认操作'); return }
          if (!demoMode.value && user.value?.password && val !== user.value.password) {
            ElMessage.error('密码错误，操作已取消')
            return
          }
          done()
        }
      })
      return true
    } catch (e) {
      if (e !== 'cancel' && e !== 'close' && !(e && typeof e === 'object' && e.action === 'cancel')) {
        console.warn('[AUTH] requireSensitiveConfirmation error:', e)
      }
      return false
    }
  }

  // === 登录 / 注册 / 重置密码 ===
  async function login(username, password, selectedRole) {
    loading.value = true
    error.value = null
    try {
      const matched = DEMO_USER_MAP[username]
      if (matched && matched.password === password) {
        const userData = {
          id: DEFAULT_ACCOUNTS.findIndex(a => a.username === username) + 1,
          username: matched.username,
          role: matched.role,
          displayName: matched.displayName,
          demo: true,
          team: DEMO_USER_TEAM_MAP[username] || 'team-a',
          dataScope: DEFAULT_DATA_SCOPE_BY_ROLE[matched.role] || 'personal',
          loginTime: new Date().toISOString()
        }
        setUser(userData)
        setDemoMode(true)
        setToken(`demo.${btoa(username)}.${Date.now()}`)
        return userData
      }
      if (selectedRole && ROLES[selectedRole.toUpperCase()]) {
        const resolvedRole = ROLES[selectedRole.toUpperCase()]
        const userData = {
          id: 100,
          username,
          role: resolvedRole,
          displayName: `${ROLE_LABELS[resolvedRole]} - ${username}`,
          demo: true,
          team: DEMO_USER_TEAM_MAP[username] || 'team-a',
          dataScope: DEFAULT_DATA_SCOPE_BY_ROLE[resolvedRole] || 'personal',
          loginTime: new Date().toISOString()
        }
        setUser(userData)
        setDemoMode(true)
        setToken(`demo.${btoa(username)}.${Date.now()}`)
        return userData
      }
      throw new Error('用户名或密码错误')
    } catch (err) {
      error.value = err?.message || '登录失败，请检查用户名和密码'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function register(username, password, selectedRole) {
    loading.value = true
    error.value = null
    try {
      if (!selectedRole || !ROLES[selectedRole.toUpperCase()]) {
        throw new Error('请选择用户类型')
      }
      if (!username || username.length < 3) throw new Error('用户名至少3个字符')
      if (!password || password.length < 6) throw new Error('密码至少6个字符')
      const resolvedRole = ROLES[selectedRole.toUpperCase()]
      const userData = {
        id: Date.now(),
        username,
        role: resolvedRole,
        displayName: `${ROLE_LABELS[resolvedRole]} - ${username}`,
        demo: true,
        team: DEMO_USER_TEAM_MAP[username] || 'team-a',
        dataScope: DEFAULT_DATA_SCOPE_BY_ROLE[resolvedRole] || 'personal',
        loginTime: new Date().toISOString()
      }
      setUser(userData)
      setDemoMode(true)
      return userData
    } catch (err) {
      error.value = err?.message || '注册失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function resetPassword(username, selectedRole, newPassword) {
    loading.value = true
    error.value = null
    try {
      if (!selectedRole || !ROLES[selectedRole.toUpperCase()]) {
        throw new Error('请选择用户类型')
      }
      if (!username) throw new Error('请输入用户名')
      if (!newPassword || newPassword.length < 6) throw new Error('新密码至少6个字符')
      return { success: true, message: '密码已重置成功，请使用新密码登录' }
    } catch (err) {
      error.value = err?.message || '重置密码失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPermissionFromNacos() {
    return { ok: true, note: 'P1 启用：当前为占位实现，后续接入真实 Nacos 接口' }
  }

  async function logout() {
    stopTokenRefreshTimer()
    tokenManager.clearToken()
    setUser(null)
  }

  return {
    // state
    user, loading, error, demoMode, demoShownOnce,
    token, tokenPayload, tokenExpiresAt,
    // computed
    isLoggedIn, role, roleLabel, roleLabelEn, displayName, username, userId,
    team, teamLabel, dataScope, dataScopeLabel,
    accessibleRoutes, tokenRemainingSeconds,
    // methods (权限)
    hasRouteAccess, hasAction, canSee, getVisibleOwnerIds,
    // methods (团队/范围)
    updateTeam, updateDataScope,
    // methods (用户)
    setUser, setToken, isTokenExpired, refreshToken,
    startTokenRefreshTimer, stopTokenRefreshTimer,
    // methods (认证)
    requireSensitiveConfirmation, initFromStorage, setDemoMode, markDemoShown,
    login, register, resetPassword, fetchPermissionFromNacos, logout
  }
})
