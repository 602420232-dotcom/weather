import { ref, reactive, onUnmounted } from 'vue'
import { useWebSocket } from './useWebSocket'

export interface WeatherData {
  timestamp: string
  temperature: number
  humidity: number
  windSpeed: number
  windDirection: number
  precipitation?: number
  pressure?: number
}

export interface DroneStatus {
  droneId: string
  latitude: number
  longitude: number
  altitude: number
  speed: number
  batteryLevel: number
  status: string
  heading?: number
}

export interface TaskUpdate {
  taskId: string
  status: string
  progress: number
  eta?: string
  message?: string
}

export interface Alert {
  alertId: string
  level: 'info' | 'warning' | 'error' | 'critical'
  message: string
  timestamp: string
  source?: string
}

export function useRealTimeData() {
  const { connected, connect, subscribe, disconnect } = useWebSocket()

  const weatherData = ref<WeatherData | null>(null)
  const droneStatusMap = reactive<Map<string, DroneStatus>>(new Map())
  const taskUpdates = ref<TaskUpdate[]>([])
  const alerts = ref<Alert[]>([])
  const planningProgress = ref(0)

  const recentWeatherData = ref<WeatherData[]>([])

  let unsubscribeWeather: (() => void) | null = null
  let unsubscribeDrones: (() => void) | null = null
  let unsubscribeTasks: (() => void) | null = null
  let unsubscribeAlerts: (() => void) | null = null
  let unsubscribePlanning: (() => void) | null = null

  function initSubscriptions() {
    unsubscribeWeather = subscribe('/topic/weather', (data) => {
      weatherData.value = data as WeatherData
      recentWeatherData.value.push(data as WeatherData)
      if (recentWeatherData.value.length > 50) {
        recentWeatherData.value.shift()
      }
      console.log('[RealTime] Weather update:', data)
    })

    unsubscribeDrones = subscribe('/topic/drones', (data) => {
      const drone = data as DroneStatus
      droneStatusMap.set(drone.droneId, drone)
      console.log('[RealTime] Drone update:', drone.droneId)
    })

    unsubscribeTasks = subscribe('/topic/tasks', (data) => {
      const task = data as TaskUpdate
      taskUpdates.value.push(task)
      if (taskUpdates.value.length > 20) {
        taskUpdates.value.shift()
      }
      console.log('[RealTime] Task update:', task.taskId)
    })

    unsubscribeAlerts = subscribe('/topic/alerts', (data) => {
      const alert = data as Alert
      alerts.value.push(alert)
      if (alerts.value.length > 30) {
        alerts.value.shift()
      }
      console.log('[RealTime] Alert:', alert.level)
    })

    unsubscribePlanning = subscribe('/topic/planning', (data) => {
      planningProgress.value = (data as any).progress || 0
      console.log('[RealTime] Planning progress:', planningProgress.value)
    })
  }

  function start(baseUrl?: string) {
    connect(baseUrl)
    setTimeout(initSubscriptions, 1000)
  }

  function stop() {
    unsubscribeWeather?.()
    unsubscribeDrones?.()
    unsubscribeTasks?.()
    unsubscribeAlerts?.()
    unsubscribePlanning?.()
    disconnect()
  }

  function clearHistory() {
    weatherData.value = null
    droneStatusMap.clear()
    taskUpdates.value = []
    alerts.value = []
    recentWeatherData.value = []
    planningProgress.value = 0
  }

  function getDroneStatus(droneId: string): DroneStatus | undefined {
    return droneStatusMap.get(droneId)
  }

  function getAllDrones(): DroneStatus[] {
    return Array.from(droneStatusMap.values())
  }

  onUnmounted(() => {
    stop()
  })

  return {
    connected,
    weatherData,
    recentWeatherData,
    droneStatusMap,
    taskUpdates,
    alerts,
    planningProgress,
    start,
    stop,
    clearHistory,
    getDroneStatus,
    getAllDrones,
  }
}
