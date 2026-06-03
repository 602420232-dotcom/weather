import { App, inject } from 'vue'
import { useWebSocket } from '../composables/useWebSocket'

const WS_KEY = 'websocket'

export function createWebSocketPlugin() {
  return {
    install(app: App) {
      const ws = useWebSocket()
      setTimeout(() => ws.connect(), 2000)
      app.provide(WS_KEY, ws)
      app.config.globalProperties.$ws = ws
    }
  }
}

export function useWs() {
  const ws = inject(WS_KEY) as ReturnType<typeof useWebSocket>
  if (!ws) throw new Error('useWs() must be used after install')
  return ws
}
