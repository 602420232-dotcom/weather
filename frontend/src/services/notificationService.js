import { useWebSocket } from '@/composables/useWebSocket'
import { ElNotification } from 'element-plus'

/**
 * 统一通知服务
 * 整合 STOMP WebSocket（useWebSocket）与业务逻辑，
 * 替换原 utils/websocket.js 的纯模拟实现。
 * 当 useWebSocket 不可用或处于演示模式时，自动回退到本地模拟。
 */
class NotificationService {
  #handlers = new Map()
  #connected = false
  #demoMode = false
  #demoTimer = null
  #wsInstance = null

  // --------------- 连接管理 ---------------

  async connect(userId, { demoMode = false } = {}) {
    this.#demoMode = demoMode

    if (demoMode) {
      this.#startDemoSimulation(userId)
      return
    }

    try {
      this.#wsInstance = useWebSocket()
      await this.#wsInstance.connect('/ws')
      this.#connected = true
      this.#dispatch({ type: 'connect', userId })
      console.log('[NotificationService] STOMP WebSocket 已连接')
    } catch (e) {
      console.warn('[NotificationService] STOMP 不可用，回退到模拟模式')
      this.#demoMode = true
      this.#startDemoSimulation(userId)
    }
  }

  disconnect() {
    this.#stopDemoSimulation()
    if (this.#wsInstance) {
      this.#wsInstance.disconnect()
      this.#wsInstance = null
    }
    this.#connected = false
    this.#dispatch({ type: 'disconnect' })
    console.log('[NotificationService] 已断开')
  }

  isConnected() {
    return this.#connected
  }

  // --------------- 演示模式模拟 ---------------

  #startDemoSimulation(userId) {
    console.log('[NotificationService] 启动演示模式通知模拟，用户:', userId)

    setTimeout(() => {
      this.#connected = true
      this.#dispatch({ type: 'connect', userId })

      this.#scheduleDemoNotification()
    }, 500)
  }

  #scheduleDemoNotification() {
    const delay = 30000 + Math.random() * 30000 // 30-60 秒
    this.#demoTimer = setTimeout(() => {
      if (!this.#connected) return

      const mockNotifications = [
        {
          id: `n_${Date.now()}`,
          type: 'reply',
          source: 'forum',
          message: '张三 回复了你的帖子',
          postTitle: '关于DE-RRT*算法参数调优的讨论',
          postId: '2',
          from: '张三',
          timestamp: new Date().toISOString()
        },
        {
          id: `n_${Date.now()}`,
          type: 'mention',
          source: 'forum',
          message: '李四 在评论中提到了你',
          postTitle: '飞控端GPS信号异常问题反馈',
          postId: '3',
          from: '李四',
          timestamp: new Date().toISOString()
        },
        {
          id: `n_${Date.now()}`,
          type: 'system',
          source: 'system',
          message: '系统公告：有新的版本更新',
          postTitle: 'V2.0版本发布公告',
          postId: '1',
          from: '系统',
          timestamp: new Date().toISOString()
        }
      ]

      const notification = {
        ...mockNotifications[Math.floor(Math.random() * mockNotifications.length)],
        id: `n_${Date.now()}`,
        timestamp: new Date().toISOString()
      }

      this.#handleMessage(notification)
      this.#scheduleDemoNotification()
    }, delay)
  }

  #stopDemoSimulation() {
    if (this.#demoTimer) {
      clearTimeout(this.#demoTimer)
      this.#demoTimer = null
    }
  }

  // --------------- 消息处理 ---------------

  #handleMessage(data) {
    console.log('[NotificationService] 收到消息:', data)
    this.#dispatch({ type: 'message', data })

    // 显示桌面通知（除非免打扰）
    try {
      const doNotDisturb = localStorage.getItem('notification_doNotDisturb')
      if (doNotDisturb === 'true') return
    } catch (_) {}

    if (data.silent) return

    const typeMap = {
      reply: { type: 'info', label: '新回复' },
      mention: { type: 'warning', label: '@提及' },
      system: { type: 'success', label: '系统通知' }
    }
    const config = typeMap[data.type] || { type: 'info', label: '新通知' }

    ElNotification({
      title: config.label,
      message: data.message || data.content,
      duration: 5000,
      type: config.type,
      position: 'top-right',
      onClick: () => {
        if (data.postId) {
          window.location.hash = `/forum?post=${data.postId}`
        }
      }
    })
  }

  // --------------- 发送消息 ---------------

  send(data) {
    if (!this.#connected) {
      console.warn('[NotificationService] 未连接，消息未发送')
      return false
    }

    try {
      if (this.#demoMode) {
        console.log('[NotificationService] [模拟] 发送消息:', data)
        if (data.type === 'comment') {
          setTimeout(() => {
            this.#dispatch({
              type: 'message',
              data: {
                type: 'confirmation',
                original: data,
                success: true,
                timestamp: new Date().toISOString()
              }
            })
          }, 100)
        }
        return true
      }

      // 真实 STOMP 发送
      if (this.#wsInstance) {
        this.#wsInstance.send('/app/message', {}, JSON.stringify(data))
      }
      return true
    } catch (error) {
      console.error('[NotificationService] 发送失败:', error)
      return false
    }
  }

  // --------------- 事件监听 ---------------

  on(event, handler) {
    if (!this.#handlers.has(event)) {
      this.#handlers.set(event, new Set())
    }
    this.#handlers.get(event).add(handler)
  }

  off(event, handler) {
    const set = this.#handlers.get(event)
    if (set) {
      set.delete(handler)
      if (set.size === 0) this.#handlers.delete(event)
    }
  }

  #dispatch(eventOrPayload) {
    // 支持两种调用形式：
    //   #dispatch({ type, ... })       — 结构化事件
    //   #dispatch('connect', payload)  — (eventName, data)
    let eventName, data
    if (typeof eventOrPayload === 'string') {
      eventName = eventOrPayload
      data = arguments[1]
    } else {
      eventName = eventOrPayload.type || 'message'
      data = eventOrPayload
    }

    const set = this.#handlers.get(eventName)
    if (set) {
      set.forEach((handler) => handler(data))
    }
  }

  // --------------- 测试工具 ---------------

  triggerNotification(notification) {
    this.#handleMessage(notification)
  }
}

// 单例
export const notificationService = new NotificationService()

// 同时导出类以便测试
export { NotificationService }
