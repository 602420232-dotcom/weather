// 性能优化工具类

/**
 * 防抖函数
 * @param {Function} func - 要执行的函数
 * @param {number} wait - 等待时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, wait = 300) {
  let timeout
  return function(...args) {
    clearTimeout(timeout)
    timeout = setTimeout(() => {
      func.apply(this, args)
    }, wait)
  }
}

/**
 * 节流函数
 * @param {Function} func - 要执行的函数
 * @param {number} limit - 时间限制（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(func, limit = 300) {
  let inThrottle
  return function(...args) {
    if (!inThrottle) {
      func.apply(this, args)
      inThrottle = true
      setTimeout(() => {
        inThrottle = false
      }, limit)
    }
  }
}

/**
 * 本地存储缓存
 */
export const storage = {
  /**
   * 设置缓存
   * @param {string} key - 缓存键
   * @param {any} value - 缓存值
   * @param {number} expire - 过期时间（毫秒）
   */
  set(key, value, expire = null) {
    const data = {
      value,
      expire: expire ? Date.now() + expire : null
    }
    localStorage.setItem(key, JSON.stringify(data))
  },
  
  /**
   * 获取缓存
   * @param {string} key - 缓存键
   * @returns {any} 缓存值
   */
  get(key) {
    const item = localStorage.getItem(key)
    if (!item) return null
    
    try {
      const data = JSON.parse(item)
      if (data.expire && Date.now() > data.expire) {
        localStorage.removeItem(key)
        return null
      }
      return data.value
    } catch (e) {
      localStorage.removeItem(key)
      return null
    }
  },
  
  /**
   * 删除缓存
   * @param {string} key - 缓存键
   */
  remove(key) {
    localStorage.removeItem(key)
  },
  
  /**
   * 清空缓存
   */
  clear() {
    localStorage.clear()
  }
}

/**
 * 图片懒加载
 * @param {HTMLElement} img - 图片元素
 * @param {string} src - 图片地址
 */
export function lazyLoadImage(img, src) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        img.src = src
        observer.unobserve(img)
      }
    })
  })
  observer.observe(img)
}

/**
 * 组件懒加载
 * @param {Function} loader - 组件加载函数
 * @returns {Promise} 组件加载Promise
 */
export function lazyLoadComponent(loader) {
  return () => {
    const component = loader()
    return Promise.resolve(component)
  }
}

/**
 * 计算性能指标
 * @returns {Object} 性能指标
 */
export function getPerformanceMetrics() {
  if (!window.performance) return null
  
  const metrics = {
    navigationStart: window.performance.timing.navigationStart,
    loadEventEnd: window.performance.timing.loadEventEnd,
    domContentLoadedEventEnd: window.performance.timing.domContentLoadedEventEnd,
    firstPaint: window.performance.getEntriesByType('paint')[0]?.startTime || 0,
    firstContentfulPaint: window.performance.getEntriesByType('paint')[1]?.startTime || 0
  }
  
  metrics.loadTime = metrics.loadEventEnd - metrics.navigationStart
  metrics.domContentLoadedTime = metrics.domContentLoadedEventEnd - metrics.navigationStart
  
  return metrics
}

/**
 * 监控性能
 * @param {Function} callback - 性能回调函数
 */
export function monitorPerformance(callback) {
  if (!window.performance) return
  
  window.addEventListener('load', () => {
    setTimeout(() => {
      const metrics = getPerformanceMetrics()
      if (callback) callback(metrics)
    }, 0)
  })
}
