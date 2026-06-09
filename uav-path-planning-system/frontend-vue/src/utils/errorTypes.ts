/**
 * 结构化错误分类体系
 * 统一应用内错误类型，配合 ErrorBoundary 和 API 拦截器使用
 */

export type ErrorLevel = 'error' | 'warning' | 'info'

export class AppError extends Error {
  code: string
  level: ErrorLevel
  timestamp: number

  constructor(message: string, code: string, level: ErrorLevel = 'error') {
    super(message)
    this.name = 'AppError'
    this.code = code
    this.level = level
    this.timestamp = Date.now()
  }
}

export class NetworkError extends AppError {
  constructor(message: string = '网络连接失败，请检查网络后重试') {
    super(message, 'NETWORK_ERROR', 'error')
    this.name = 'NetworkError'
  }
}

export class AuthError extends AppError {
  constructor(message: string = '登录已过期，请重新登录') {
    super(message, 'AUTH_ERROR', 'error')
    this.name = 'AuthError'
  }
}

export class ValidationError extends AppError {
  field?: string

  constructor(message: string, field?: string) {
    super(message, 'VALIDATION_ERROR', 'warning')
    this.name = 'ValidationError'
    this.field = field
  }
}

export class BusinessError extends AppError {
  constructor(message: string, code: string = 'BUSINESS_ERROR') {
    super(message, code, 'warning')
    this.name = 'BusinessError'
  }
}

/** 错误类型 -> 用户提示映射 */
export const ERROR_MESSAGES: Record<string, string> = {
  NETWORK_ERROR: '网络连接失败，请检查网络后重试',
  AUTH_ERROR: '登录已过期，请重新登录',
  VALIDATION_ERROR: '输入数据校验失败',
  RATE_LIMIT: '操作过于频繁，请稍后再试',
  SERVER_ERROR: '服务器繁忙，请稍后重试',
  NOT_FOUND: '请求的资源不存在',
  FORBIDDEN: '您没有权限执行此操作',
  BACKEND_UNAVAILABLE: '后端服务未连接，请检查服务状态',
  UNKNOWN: '发生未知错误，请稍后再试'
}

/**
 * 根据 HTTP 状态码创建对应的 AppError 子类
 */
export function createHttpError(status: number, message?: string): AppError {
  const msg = message || ERROR_MESSAGES.UNKNOWN
  switch (status) {
    case 400: return new BusinessError(msg, 'BAD_REQUEST')
    case 401: return new AuthError(msg)
    case 403: return new BusinessError(msg, 'FORBIDDEN')
    case 404: return new BusinessError(msg, 'NOT_FOUND')
    case 429: return new BusinessError(msg, 'RATE_LIMIT')
    case 500:
    case 502:
    case 503:
    case 504: return new BusinessError(msg, 'SERVER_ERROR')
    default:
      if (status >= 400 && status < 500) return new BusinessError(msg, 'CLIENT_ERROR')
      if (status >= 500) return new BusinessError(msg, 'SERVER_ERROR')
      return new AppError(msg, 'UNKNOWN', 'error')
  }
}

/**
 * 判断错误是否可重试
 * 网络错误和服务器错误通常可以重试
 */
export function isRetryableError(err: any): boolean {
  if (err instanceof NetworkError) return true
  if (err instanceof AppError && err.code === 'SERVER_ERROR') return true
  if (err.code === 'ECONNABORTED') return true
  if (err.message === 'Network Error') return true
  return false
}

/**
 * 获取错误对用户的友好提示
 */
export function getUserFriendlyMessage(err: any): string {
  if (err instanceof AppError) {
    return err.message
  }
  if (ERROR_MESSAGES[err.code]) {
    return ERROR_MESSAGES[err.code]
  }
  return err.message || ERROR_MESSAGES.UNKNOWN
}
