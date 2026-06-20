import { ref } from 'vue'

type WsCallback = (data: any) => void

export function useWebSocket() {
  const connected = ref(false)
  let stompClient: any = null
  let subscriptions: Map<string, any> = new Map()

  async function connect(baseUrl?: string) {
    if (stompClient?.connected) return

    try {
      const { Client } = await import('@stomp/stompjs')
      const SockJS = (await import('sockjs-client')).default

      const wsUrl = baseUrl || window.location.origin + '/ws'

      stompClient = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          connected.value = true
          console.log('[WS] 已连接:', wsUrl)
        },
        onDisconnect: () => {
          connected.value = false
          console.log('[WS] 已断开')
        },
      })

      stompClient.activate()
    } catch (e) {
      console.warn('[WS] 加载失败:', e)
    }
  }

  function subscribe(topic: string, callback: WsCallback) {
    if (!stompClient?.connected) {
      console.warn('[WS] 未连接')
      return () => {}
    }

    const sub = stompClient.subscribe(topic, (msg: any) => {
      try {
        callback(JSON.parse(msg.body))
      } catch {
        callback(msg.body)
      }
    })
    subscriptions.set(topic, sub)

    return () => {
      sub.unsubscribe()
      subscriptions.delete(topic)
    }
  }

  function disconnect() {
    stompClient?.deactivate()
    stompClient = null
    connected.value = false
  }

  return { connected, connect, subscribe, disconnect }
}
