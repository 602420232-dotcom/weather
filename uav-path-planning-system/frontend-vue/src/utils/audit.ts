import { useAuditStore } from '../stores/audit'

export const AUDIT_ACTIONS: Record<string, string> = {
  LOGIN: 'LOGIN',
  LOGIN_FAILED: 'LOGIN_FAILED',
  LOGOUT: 'LOGOUT',
  SUBMIT_ORDER: 'SUBMIT_ORDER',
  SUBMIT_REPORT: 'SUBMIT_REPORT',
  CANCEL_REPORT: 'CANCEL_REPORT',
  SWITCH_PRODUCTION: 'SWITCH_PRODUCTION',
  CREATE_USER: 'CREATE_USER',
  DELETE_USER: 'DELETE_USER',
  MODIFY_PERMISSION: 'MODIFY_PERMISSION',
  MODIFY_API_CONFIG: 'MODIFY_API_CONFIG',
  VIEW_SENSITIVE: 'VIEW_SENSITIVE',
  UTM_SYNC: 'UTM_SYNC',
  TASK_EXECUTE: 'TASK_EXECUTE',
  CHANGE_TEAM: 'CHANGE_TEAM',
  CHANGE_DATA_SCOPE: 'CHANGE_DATA_SCOPE',
  OTHER: 'OTHER'
}

export interface AuditLogOptions {
  user?: string | null
  role?: string | null
  action?: string
  target?: string | null
  detail?: string | null
  level?: 'info' | 'warning' | 'error' | 'critical'
}

export interface AuditLogEntry {
  user: string
  role: string
  action: string
  target: string
  detail: string
  level: 'info' | 'warning' | 'error' | 'critical'
  timestamp: string
  ip: string
}

export function logAction({
  user = null,
  role = null,
  action = AUDIT_ACTIONS.OTHER,
  target = null,
  detail = null,
  level = 'info'
}: AuditLogOptions = {}): AuditLogEntry | null {
  try {
    const auditStore = useAuditStore()
    const entry: AuditLogEntry = {
      user: user || (auditStore.currentUser || 'anonymous'),
      role: role || (auditStore.currentRole || 'anonymous'),
      action,
      target: target || '-',
      detail: detail || '',
      level: ['info', 'warning', 'error', 'critical'].includes(level) ? level : 'info',
      timestamp: new Date().toISOString(),
      ip: typeof window !== 'undefined' ? (window.location?.hostname || 'local') : 'local'
    }
    auditStore.addLog(entry)
    console.log('[AUDIT]', `[${entry.level.toUpperCase()}]`, entry.action, entry.user, entry.target, entry.detail || '')
    return entry
  } catch (e) {
    console.warn('[AUDIT] 审计写入失败:', e)
    return null
  }
}

export default { logAction, AUDIT_ACTIONS }
