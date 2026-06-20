/**
 * 权限管理 Store
 * 负责权限矩阵维护、路由/操作权限检查、菜单过滤
 */
import { defineStore } from 'pinia'
import { ref, Ref } from 'vue'
import { ROLES } from './constants/roles'
import { PERMISSION_MATRIX, ACTION_PERMISSIONS } from './constants/permissionsMatrix'

const NACOS_PERMISSION_KEY = 'uav_nacos_permission_matrix'
const NACOS_FETCHED_KEY = 'uav_nacos_fetched'

type RouteKey = string
type RoleKey = string
type ActionKey = string

export interface MenuItem {
  key: RouteKey
  [key: string]: any
}

export const usePermissionStore = defineStore('permissions', () => {
  const matrix: Ref<Record<RoleKey, RouteKey[]>> = ref({ ...PERMISSION_MATRIX })
  const nacosFetched: Ref<boolean> = ref(false)

  /** 检查角色是否有路由访问权限 */
  function hasRouteAccess(role: RoleKey, routeKey: RouteKey): boolean {
    if (!role) return false
    return (matrix.value[role] || []).includes(routeKey)
  }

  /** 检查角色是否有操作权限 */
  function hasAction(role: RoleKey, actionKey: ActionKey): boolean {
    if (!role) return false
    const allowed = ACTION_PERMISSIONS[actionKey]
    if (!allowed) return false
    return allowed.includes(role)
  }

  /** 获取角色可访问的路由 key 列表 */
  function getAccessibleRoutes(role: RoleKey): RouteKey[] {
    return matrix.value[role] || []
  }

  /** 根据角色过滤菜单项 */
  function filterMenuItems(role: RoleKey, allItems: MenuItem[]): MenuItem[] {
    if (!role) return []
    return allItems.filter(item => hasRouteAccess(role, item.key))
  }

  /** 合并 Nacos 动态权限 */
  function mergeNacosMatrix(nacosData: Record<RoleKey, RouteKey[]> | null): void {
    if (!nacosData || typeof nacosData !== 'object') return
    Object.keys(nacosData).forEach((role: RoleKey) => {
      if (matrix.value[role]) {
        const merged = new Set([...matrix.value[role], ...(nacosData[role] || [])])
        matrix.value[role] = Array.from(merged)
      } else {
        matrix.value[role] = [...(nacosData[role] || [])]
      }
    })
    nacosFetched.value = true
    try {
      localStorage.setItem(NACOS_PERMISSION_KEY, JSON.stringify(nacosData))
      localStorage.setItem(NACOS_FETCHED_KEY, 'true')
    } catch (e) {}
  }

  /** 从 localStorage 恢复 Nacos 权限 */
  function restoreNacosMatrix(): boolean {
    try {
      const raw = localStorage.getItem(NACOS_PERMISSION_KEY)
      if (raw) {
        const data: Record<RoleKey, RouteKey[]> = JSON.parse(raw)
        mergeNacosMatrix(data)
        return true
      }
    } catch (e) {
      console.warn('Failed to restore Nacos permission matrix', e)
    }
    return false
  }

  /** 重置为本地默认矩阵 */
  function resetToDefault(): void {
    matrix.value = { ...PERMISSION_MATRIX }
    nacosFetched.value = false
    try {
      localStorage.removeItem(NACOS_PERMISSION_KEY)
      localStorage.removeItem(NACOS_FETCHED_KEY)
    } catch (e) {}
  }

  /** 获取所有角色列表 */
  function getRoles(): string[] {
    return Object.values(ROLES)
  }

  return {
    matrix,
    nacosFetched,
    hasRouteAccess,
    hasAction,
    getAccessibleRoutes,
    filterMenuItems,
    mergeNacosMatrix,
    restoreNacosMatrix,
    resetToDefault,
    getRoles
  }
})
