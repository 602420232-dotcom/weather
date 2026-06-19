// 气象数据源管理 API
// 管理多种气象数据源：浮标、无人机、地面站、探空仪、卫星

// 数据源类型定义
export const DATA_SOURCE_TYPES = {
  BUOY: 'buoy',
  DETECTION_DRONE: 'detection-drone',
  GROUND_STATION: 'ground-station',
  RADIOSONDE: 'radiosonde',
  SATELLITE: 'satellite'
}

// 数据源类型标签
export const DATA_SOURCE_LABELS = {
  [DATA_SOURCE_TYPES.BUOY]: '浮标气象',
  [DATA_SOURCE_TYPES.DETECTION_DRONE]: '探测无人机',
  [DATA_SOURCE_TYPES.GROUND_STATION]: '地面气象站',
  [DATA_SOURCE_TYPES.RADIOSONDE]: '无线电探空仪',
  [DATA_SOURCE_TYPES.SATELLITE]: '卫星气象'
}

// 数据源类型图标
export const DATA_SOURCE_ICONS = {
  [DATA_SOURCE_TYPES.BUOY]: '🌊',
  [DATA_SOURCE_TYPES.DETECTION_DRONE]: '🚁',
  [DATA_SOURCE_TYPES.GROUND_STATION]: '🏠',
  [DATA_SOURCE_TYPES.RADIOSONDE]: '🎈',
  [DATA_SOURCE_TYPES.SATELLITE]: '🛰️'
}

// 模拟数据源列表
const MOCK_DATA_SOURCES = [
  {
    id: 'buoy-01',
    name: '东海浮标站 01',
    type: DATA_SOURCE_TYPES.BUOY,
    location: '东海海域 A1',
    coordinates: { lat: 31.5, lng: 122.5 },
    status: 'online',
    lastUpdate: '2026-06-09T14:35:00Z',
    uptime: 99.8,
    dataQuality: {
      completeness: 98.5,
      latency: 2.3,
      errorRate: 0.2
    },
    config: {
      endpoint: 'ws://buoy-01.weather.local:8080',
      interval: 30000,
      protocol: 'MQTT'
    },
    latestData: {
      waterTemp: 24.5,
      airTemp: 26.2,
      humidity: 78,
      windSpeed: 5.2,
      windDirection: 'NE',
      waveHeight: 1.2,
      pressure: 1008
    }
  },
  {
    id: 'drone-01',
    name: '气象探测无人机 01',
    type: DATA_SOURCE_TYPES.DETECTION_DRONE,
    location: '成都附近空域',
    coordinates: { lat: 30.6, lng: 104.0 },
    status: 'online',
    lastUpdate: '2026-06-09T14:34:00Z',
    uptime: 96.5,
    dataQuality: {
      completeness: 95.2,
      latency: 5.1,
      errorRate: 1.3
    },
    config: {
      endpoint: 'mqtt://drone-01.weather.local:1883',
      interval: 60000,
      protocol: 'MQTT'
    },
    latestData: {
      altitude: 500,
      airTemp: 22.8,
      humidity: 65,
      windSpeed: 3.5,
      windDirection: 'E',
      pressure: 850
    }
  },
  {
    id: 'ground-01',
    name: '气象地面站 01',
    type: DATA_SOURCE_TYPES.GROUND_STATION,
    location: '成都气象局',
    coordinates: { lat: 30.65, lng: 104.06 },
    status: 'online',
    lastUpdate: '2026-06-09T14:35:30Z',
    uptime: 99.9,
    dataQuality: {
      completeness: 99.8,
      latency: 1.2,
      errorRate: 0.05
    },
    config: {
      endpoint: 'http://ground-01.weather.local:9000/api',
      interval: 10000,
      protocol: 'HTTP'
    },
    latestData: {
      airTemp: 28.5,
      humidity: 62,
      windSpeed: 2.1,
      windDirection: 'SE',
      pressure: 1005,
      visibility: 8.5,
      rainfall: 0
    }
  },
  {
    id: 'ground-02',
    name: '气象地面站 02',
    type: DATA_SOURCE_TYPES.GROUND_STATION,
    location: '杭州气象局',
    coordinates: { lat: 30.25, lng: 120.18 },
    status: 'offline',
    lastUpdate: '2026-06-09T12:30:00Z',
    uptime: 85.2,
    dataQuality: {
      completeness: 72.3,
      latency: null,
      errorRate: null
    },
    config: {
      endpoint: 'http://ground-02.weather.local:9000/api',
      interval: 10000,
      protocol: 'HTTP'
    },
    latestData: null
  },
  {
    id: 'radiosonde-01',
    name: '探空站 01',
    type: DATA_SOURCE_TYPES.RADIOSONDE,
    location: '成都探空站',
    coordinates: { lat: 30.62, lng: 104.02 },
    status: 'online',
    lastUpdate: '2026-06-09T14:00:00Z',
    uptime: 98.1,
    dataQuality: {
      completeness: 96.8,
      latency: 8.5,
      errorRate: 0.8
    },
    config: {
      endpoint: 'ws://radiosonde-01.weather.local:7070',
      interval: 1800000,
      protocol: 'WebSocket'
    },
    latestData: {
      levels: 12,
      maxAltitude: 25000,
      surfaceTemp: 28.5,
      surfacePressure: 1005,
      tropopauseAltitude: 15000,
      tropopauseTemp: -65
    }
  },
  {
    id: 'satellite-01',
    name: '气象卫星 01 (FY-4A)',
    type: DATA_SOURCE_TYPES.SATELLITE,
    location: '地球同步轨道',
    coordinates: { lat: 104.7, lng: 0 },
    status: 'online',
    lastUpdate: '2026-06-09T14:34:45Z',
    uptime: 99.5,
    dataQuality: {
      completeness: 97.2,
      latency: 15.0,
      errorRate: 0.5
    },
    config: {
      endpoint: 'wss://satellite-01.weather.local:8443',
      interval: 15000,
      protocol: 'WebSocket'
    },
    latestData: {
      channels: 14,
      resolution: '1km',
      cloudCover: 45,
      lastImageTime: '2026-06-09T14:30:00Z'
    }
  },
  {
    id: 'satellite-02',
    name: '气象卫星 02 (NOAA-20)',
    type: DATA_SOURCE_TYPES.SATELLITE,
    location: '太阳同步轨道',
    coordinates: { lat: null, lng: null },
    status: 'online',
    lastUpdate: '2026-06-09T14:32:00Z',
    uptime: 99.2,
    dataQuality: {
      completeness: 94.5,
      latency: 25.0,
      errorRate: 1.2
    },
    config: {
      endpoint: 'wss://satellite-02.weather.local:8443',
      interval: 60000,
      protocol: 'WebSocket'
    },
    latestData: {
      channels: 22,
      resolution: '750m',
      lastPassTime: '2026-06-09T08:15:00Z',
      coverage: '全球'
    }
  }
]

// 数据源统计
const getDataSourceStats = () => {
  const total = MOCK_DATA_SOURCES.length
  const online = MOCK_DATA_SOURCES.filter(ds => ds.status === 'online').length
  const offline = total - online
  
  // 按类型统计
  const byType = {}
  Object.values(DATA_SOURCE_TYPES).forEach(type => {
    const sources = MOCK_DATA_SOURCES.filter(ds => ds.type === type)
    byType[type] = {
      total: sources.length,
      online: sources.filter(ds => ds.status === 'online').length,
      offline: sources.filter(ds => ds.status === 'offline').length
    }
  })
  
  // 平均数据质量
  const onlineSources = MOCK_DATA_SOURCES.filter(ds => ds.status === 'online')
  const avgCompleteness = onlineSources.reduce((sum, ds) => sum + ds.dataQuality.completeness, 0) / onlineSources.length
  const avgLatency = onlineSources.reduce((sum, ds) => sum + ds.dataQuality.latency, 0) / onlineSources.length
  
  return {
    total,
    online,
    offline,
    uptime: ((online / total) * 100).toFixed(1),
    byType,
    avgCompleteness: avgCompleteness.toFixed(1),
    avgLatency: avgLatency.toFixed(1)
  }
}

const weatherSourceApi = {
  // 获取所有数据源
  getAllSources() {
    return Promise.resolve([...MOCK_DATA_SOURCES])
  },

  // 获取数据源详情
  getSource(id) {
    const source = MOCK_DATA_SOURCES.find(ds => ds.id === id)
    if (!source) {
      return Promise.reject(new Error('Data source not found'))
    }
    return Promise.resolve({ ...source })
  },

  // 获取数据源列表（支持筛选）
  getSources(params = {}) {
    let sources = [...MOCK_DATA_SOURCES]
    
    if (params.type) {
      sources = sources.filter(ds => ds.type === params.type)
    }
    
    if (params.status) {
      sources = sources.filter(ds => ds.status === params.status)
    }
    
    return Promise.resolve(sources)
  },

  // 获取数据源统计
  getStats() {
    return Promise.resolve(getDataSourceStats())
  },

  // 更新数据源配置
  updateConfig(id, config) {
    const index = MOCK_DATA_SOURCES.findIndex(ds => ds.id === id)
    if (index === -1) {
      return Promise.reject(new Error('Data source not found'))
    }
    
    MOCK_DATA_SOURCES[index].config = {
      ...MOCK_DATA_SOURCES[index].config,
      ...config
    }
    
    return Promise.resolve(MOCK_DATA_SOURCES[index])
  },

  // 切换数据源状态（模拟）
  toggleSource(id) {
    const index = MOCK_DATA_SOURCES.findIndex(ds => ds.id === id)
    if (index === -1) {
      return Promise.reject(new Error('Data source not found'))
    }
    
    MOCK_DATA_SOURCES[index].status = 
      MOCK_DATA_SOURCES[index].status === 'online' ? 'offline' : 'online'
    
    if (MOCK_DATA_SOURCES[index].status === 'online') {
      MOCK_DATA_SOURCES[index].lastUpdate = new Date().toISOString()
    }
    
    return Promise.resolve(MOCK_DATA_SOURCES[index])
  },

  // 获取数据类型对应的字段描述
  getDataFields(type) {
    const fields = {
      [DATA_SOURCE_TYPES.BUOY]: [
        { key: 'waterTemp', label: '水温', unit: '°C' },
        { key: 'airTemp', label: '气温', unit: '°C' },
        { key: 'humidity', label: '湿度', unit: '%' },
        { key: 'windSpeed', label: '风速', unit: 'm/s' },
        { key: 'windDirection', label: '风向' },
        { key: 'waveHeight', label: '波高', unit: 'm' },
        { key: 'pressure', label: '气压', unit: 'hPa' }
      ],
      [DATA_SOURCE_TYPES.DETECTION_DRONE]: [
        { key: 'altitude', label: '飞行高度', unit: 'm' },
        { key: 'airTemp', label: '气温', unit: '°C' },
        { key: 'humidity', label: '湿度', unit: '%' },
        { key: 'windSpeed', label: '风速', unit: 'm/s' },
        { key: 'windDirection', label: '风向' },
        { key: 'pressure', label: '气压', unit: 'hPa' }
      ],
      [DATA_SOURCE_TYPES.GROUND_STATION]: [
        { key: 'airTemp', label: '气温', unit: '°C' },
        { key: 'humidity', label: '湿度', unit: '%' },
        { key: 'windSpeed', label: '风速', unit: 'm/s' },
        { key: 'windDirection', label: '风向' },
        { key: 'pressure', label: '气压', unit: 'hPa' },
        { key: 'visibility', label: '能见度', unit: 'km' },
        { key: 'rainfall', label: '降雨量', unit: 'mm' }
      ],
      [DATA_SOURCE_TYPES.RADIOSONDE]: [
        { key: 'levels', label: '探测层级', unit: '' },
        { key: 'maxAltitude', label: '最高高度', unit: 'm' },
        { key: 'surfaceTemp', label: '地面温度', unit: '°C' },
        { key: 'surfacePressure', label: '地面气压', unit: 'hPa' },
        { key: 'tropopauseAltitude', label: '对流层顶高度', unit: 'm' },
        { key: 'tropopauseTemp', label: '对流层顶温度', unit: '°C' }
      ],
      [DATA_SOURCE_TYPES.SATELLITE]: [
        { key: 'channels', label: '观测通道', unit: '' },
        { key: 'resolution', label: '分辨率' },
        { key: 'cloudCover', label: '云覆盖率', unit: '%' },
        { key: 'lastImageTime', label: '最新图像时间' },
        { key: 'coverage', label: '覆盖范围' }
      ]
    }
    return fields[type] || []
  },

  // 获取数据源历史数据（模拟）
  getHistoricalData(id, params = {}) {
    const source = MOCK_DATA_SOURCES.find(ds => ds.id === id)
    if (!source) {
      return Promise.reject(new Error('Data source not found'))
    }
    
    // 生成模拟历史数据
    const duration = params.duration || 3600000 // 默认1小时
    const interval = params.interval || 60000 // 默认1分钟
    const points = Math.floor(duration / interval)
    
    const data = []
    for (let i = 0; i < points; i++) {
      const timestamp = new Date(Date.now() - (points - i) * interval).toISOString()
      const point = { timestamp }
      
      // 根据数据源类型添加随机数据
      if (source.type === DATA_SOURCE_TYPES.BUOY) {
        point.waterTemp = (source.latestData?.waterTemp || 24) + (Math.random() - 0.5) * 2
        point.airTemp = (source.latestData?.airTemp || 26) + (Math.random() - 0.5) * 2
      } else if (source.type === DATA_SOURCE_TYPES.GROUND_STATION) {
        point.airTemp = (source.latestData?.airTemp || 28) + (Math.random() - 0.5) * 3
        point.humidity = Math.min(100, Math.max(0, (source.latestData?.humidity || 60) + (Math.random() - 0.5) * 10))
      }
      
      data.push(point)
    }
    
    return Promise.resolve(data)
  }
}

export default weatherSourceApi