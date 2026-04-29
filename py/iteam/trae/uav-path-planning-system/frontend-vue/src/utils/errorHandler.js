// 错误处理和容错机制工具类

/**
 * 全局错误处理器
 */
export class ErrorHandler {
  constructor() {
    this.errors = []
    this.listeners = []
  }

  /**
   * 注册错误监听器
   * @param {Function} listener - 错误监听器函数
   */
  onError(listener) {
    this.listeners.push(listener)
  }

  /**
   * 移除错误监听器
   * @param {Function} listener - 错误监听器函数
   */
  offError(listener) {
    this.listeners = this.listeners.filter(l => l !== listener)
  }

  /**
   * 处理错误
   * @param {Error} error - 错误对象
   * @param {string} context - 错误上下文
   * @param {Object} extra - 额外信息
   */
  handleError(error, context = 'unknown', extra = {}) {
    const errorInfo = {
      timestamp: new Date().toISOString(),
      error: {
        message: error.message,
        stack: error.stack,
        name: error.name
      },
      context,
      extra
    }

    this.errors.push(errorInfo)
    this.notifyListeners(errorInfo)
    this.logError(errorInfo)

    return errorInfo
  }

  /**
   * 通知错误监听器
   * @param {Object} errorInfo - 错误信息
   */
  notifyListeners(errorInfo) {
    this.listeners.forEach(listener => {
      try {
        listener(errorInfo)
      } catch (e) {
        console.error('Error in error listener:', e)
      }
    })
  }

  /**
   * 记录错误
   * @param {Object} errorInfo - 错误信息
   */
  logError(errorInfo) {
    console.error('Error occurred:', errorInfo)
    // 这里可以添加远程日志记录
  }

  /**
   * 获取错误列表
   * @returns {Array} 错误列表
   */
  getErrors() {
    return this.errors
  }

  /**
   * 清空错误列表
   */
  clearErrors() {
    this.errors = []
  }
}

// 全局错误处理器实例
export const errorHandler = new ErrorHandler()

/**
 * 网络请求错误处理
 * @param {Error} error - 错误对象
 * @returns {Object} 错误处理结果
 */
export function handleNetworkError(error) {
  let message = '网络请求失败'
  let code = 'NETWORK_ERROR'

  if (error.response) {
    // 服务器返回错误状态码
    const status = error.response.status
    message = error.response.data?.message || `服务器错误 (${status})`
    code = `HTTP_${status}`
  } else if (error.request) {
    // 请求已发送但没有收到响应
    message = '服务器无响应，请检查网络连接'
    code = 'NO_RESPONSE'
  } else {
    // 请求配置错误
    message = error.message || '请求配置错误'
    code = 'REQUEST_ERROR'
  }

  return {
    success: false,
    message,
    code,
    error
  }
}

/**
 * 重试函数
 * @param {Function} func - 要重试的函数
 * @param {number} maxAttempts - 最大重试次数
 * @param {number} delay - 重试延迟（毫秒）
 * @returns {Promise} 重试结果
 */
export async function retry(func, maxAttempts = 3, delay = 1000) {
  let attempts = 0
  
  while (attempts < maxAttempts) {
    try {
      return await func()
    } catch (error) {
      attempts++
      if (attempts >= maxAttempts) {
        throw error
      }
      await new Promise(resolve => setTimeout(resolve, delay * attempts))
    }
  }
}

/**
 * 安全执行函数
 * @param {Function} func - 要执行的函数
 * @param {any} defaultValue - 默认值
 * @returns {any} 执行结果或默认值
 */
export function safeExecute(func, defaultValue = null) {
  try {
    return func()
  } catch (error) {
    errorHandler.handleError(error, 'safeExecute')
    return defaultValue
  }
}

/**
 * 组件错误边界
 * @param {Object} options - 选项
 * @returns {Object} 错误边界组件
 */
export function createErrorBoundary(options = {}) {
  const {
    fallbackComponent = null,
    onError = null
  } = options

  return {
    data() {
      return {
        hasError: false,
        error: null
      }
    },
    errorCaptured(error, instance, info) {
      this.hasError = true
      this.error = error
      
      if (onError) {
        onError(error, instance, info)
      }
      
      errorHandler.handleError(error, 'component', {
        component: instance.$options.name,
        info
      })
      
      return false
    },
    render(h) {
      if (this.hasError) {
        if (fallbackComponent) {
          return h(fallbackComponent, {
            props: {
              error: this.error
            }
          })
        }
        return h('div', {
          class: 'error-boundary'
        }, [
          h('h3', '组件加载失败'),
          h('p', this.error?.message || '未知错误')
        ])
      }
      return this.$slots.default ? this.$slots.default() : null
    }
  }
}

/**
 * 日志记录器
 */
export class Logger {
  constructor(name) {
    this.name = name
  }

  /**
   * 记录信息
   * @param {string} message - 日志信息
   * @param {Object} data - 附加数据
   */
  info(message, data = {}) {
    console.info(`[${this.name}] ${message}`, data)
  }

  /**
   * 记录警告
   * @param {string} message - 警告信息
   * @param {Object} data - 附加数据
   */
  warn(message, data = {}) {
    console.warn(`[${this.name}] ${message}`, data)
  }

  /**
   * 记录错误
   * @param {string} message - 错误信息
   * @param {Error} error - 错误对象
   * @param {Object} data - 附加数据
   */
  error(message, error = null, data = {}) {
    console.error(`[${this.name}] ${message}`, error, data)
    if (error) {
      errorHandler.handleError(error, this.name, data)
    }
  }

  /**
   * 记录调试信息
   * @param {string} message - 调试信息
   * @param {Object} data - 附加数据
   */
  debug(message, data = {}) {
    if (process.env.NODE_ENV === 'development') {
      console.debug(`[${this.name}] ${message}`, data)
    }
  }
}

/**
 * 创建日志记录器
 * @param {string} name - 日志记录器名称
 * @returns {Logger} 日志记录器实例
 */
export function createLogger(name) {
  return new Logger(name)
}
