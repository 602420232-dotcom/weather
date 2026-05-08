// 可视化工具类

import * as echarts from 'echarts'
import L from 'leaflet'

/**
 * 初始化地图
 * @param {string} containerId - 容器ID
 * @param {Object} options - 配置选项
 * @returns {L.Map} 地图实例
 */
export function initMap(containerId, options = {}) {
  const defaultOptions = {
    center: [39.9042, 116.4074],
    zoom: 13,
    minZoom: 10,
    maxZoom: 18
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  
  const map = L.map(containerId).setView(mergedOptions.center, mergedOptions.zoom)
  
  // 添加底图
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(map)
  
  return map
}

/**
 * 添加无人机标记
 * @param {L.Map} map - 地图实例
 * @param {Object} drone - 无人机数据
 * @returns {L.Marker} 标记实例
 */
export function addDroneMarker(map, drone) {
  const icon = L.divIcon({
    className: 'drone-marker',
    html: `<div style="width: 30px; height: 30px; background: #1890ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold;">${drone.id.charAt(0)}</div>`,
    iconSize: [30, 30]
  })
  
  const marker = L.marker([drone.latitude, drone.longitude], { icon })
    .addTo(map)
    .bindPopup(`<b>无人机 ${drone.id}</b><br>状态: ${drone.status}<br>电量: ${drone.batteryLevel}%`)
  
  return marker
}

/**
 * 添加路径
 * @param {L.Map} map - 地图实例
 * @param {Array} path - 路径点数组
 * @param {Object} options - 配置选项
 * @returns {L.Polyline} 路径实例
 */
export function addPath(map, path, options = {}) {
  const defaultOptions = {
    color: '#1890ff',
    weight: 3,
    opacity: 0.8
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  
  const polyline = L.polyline(path, mergedOptions).addTo(map)
  
  return polyline
}

/**
 * 添加任务点
 * @param {L.Map} map - 地图实例
 * @param {Object} task - 任务数据
 * @returns {L.Marker} 标记实例
 */
export function addTaskMarker(map, task) {
  const icon = L.divIcon({
    className: 'task-marker',
    html: `<div style="width: 20px; height: 20px; background: #52c41a; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold;">${task.id.charAt(0)}</div>`,
    iconSize: [20, 20]
  })
  
  const marker = L.marker([task.location[0], task.location[1]], { icon })
    .addTo(map)
    .bindPopup(`<b>任务 ${task.id}</b><br>需求: ${task.demand}<br>时间窗: ${task.startTime} - ${task.endTime}`)
  
  return marker
}

/**
 * 添加障碍物
 * @param {L.Map} map - 地图实例
 * @param {Object} obstacle - 障碍物数据
 * @returns {L.Circle} 圆形实例
 */
export function addObstacle(map, obstacle) {
  const circle = L.circle([obstacle.location[0], obstacle.location[1]], {
    color: '#ff4d4f',
    fillColor: '#ff4d4f',
    fillOpacity: 0.5,
    radius: obstacle.radius
  }).addTo(map)
    .bindPopup(`<b>障碍物</b><br>半径: ${obstacle.radius}m`)
  
  return circle
}

/**
 * 初始化折线图
 * @param {string} containerId - 容器ID
 * @param {Object} options - 配置选项
 * @returns {echarts.ECharts} 图表实例
 */
export function initLineChart(containerId, options = {}) {
  const chart = echarts.init(document.getElementById(containerId))
  
  const defaultOptions = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['数据']
    },
    xAxis: {
      type: 'category',
      data: []
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      name: '数据',
      type: 'line',
      data: []
    }]
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  chart.setOption(mergedOptions)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
  
  return chart
}

/**
 * 初始化柱状图
 * @param {string} containerId - 容器ID
 * @param {Object} options - 配置选项
 * @returns {echarts.ECharts} 图表实例
 */
export function initBarChart(containerId, options = {}) {
  const chart = echarts.init(document.getElementById(containerId))
  
  const defaultOptions = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['数据']
    },
    xAxis: {
      type: 'category',
      data: []
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      name: '数据',
      type: 'bar',
      data: []
    }]
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  chart.setOption(mergedOptions)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
  
  return chart
}

/**
 * 初始化饼图
 * @param {string} containerId - 容器ID
 * @param {Object} options - 配置选项
 * @returns {echarts.ECharts} 图表实例
 */
export function initPieChart(containerId, options = {}) {
  const chart = echarts.init(document.getElementById(containerId))
  
  const defaultOptions = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      name: '数据',
      type: 'pie',
      radius: '50%',
      data: [],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  chart.setOption(mergedOptions)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
  
  return chart
}

/**
 * 初始化热力图
 * @param {string} containerId - 容器ID
 * @param {Object} options - 配置选项
 * @returns {echarts.ECharts} 图表实例
 */
export function initHeatmapChart(containerId, options = {}) {
  const chart = echarts.init(document.getElementById(containerId))
  
  const defaultOptions = {
    tooltip: {
      position: 'top'
    },
    grid: {
      height: '50%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: [],
      splitArea: {
        show: true
      }
    },
    yAxis: {
      type: 'category',
      data: [],
      splitArea: {
        show: true
      }
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%'
    },
    series: [{
      name: '热力图',
      type: 'heatmap',
      data: [],
      label: {
        show: true
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  chart.setOption(mergedOptions)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
  
  return chart
}

/**
 * 初始化雷达图
 * @param {string} containerId - 容器ID
 * @param {Object} options - 配置选项
 * @returns {echarts.ECharts} 图表实例
 */
export function initRadarChart(containerId, options = {}) {
  const chart = echarts.init(document.getElementById(containerId))
  
  const defaultOptions = {
    tooltip: {},
    legend: {
      data: ['数据']
    },
    radar: {
      indicator: []
    },
    series: [{
      name: '数据',
      type: 'radar',
      data: []
    }]
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  chart.setOption(mergedOptions)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
  
  return chart
}

/**
 * 初始化仪表盘
 * @param {string} containerId - 容器ID
 * @param {Object} options - 配置选项
 * @returns {echarts.ECharts} 图表实例
 */
export function initGaugeChart(containerId, options = {}) {
  const chart = echarts.init(document.getElementById(containerId))
  
  const defaultOptions = {
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 100,
      splitNumber: 8,
      axisLine: {
        lineStyle: {
          width: 6,
          color: [[0.3, '#67e0e3'], [0.7, '#37a2da'], [1, '#67e0e3']]
        }
      },
      pointer: {
        icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
        length: '12%',
        width: 20,
        offsetCenter: [0, '-60%'],
        itemStyle: {
          color: 'inherit'
        }
      },
      axisTick: {
        length: 12,
        lineStyle: {
          color: 'inherit',
          width: 2
        }
      },
      splitLine: {
        length: 20,
        lineStyle: {
          color: 'inherit',
          width: 5
        }
      },
      axisLabel: {
        color: '#464646',
        fontSize: 16,
        distance: -60,
        formatter: function (value) {
          if (value === 0 || value === 100) {
            return value;
          }
          return '';
        }
      },
      title: {
        offsetCenter: [0, '-10%'],
        fontSize: 20
      },
      detail: {
        fontSize: 30,
        offsetCenter: [0, '-35%'],
        valueAnimation: true,
        formatter: function (value) {
          return Math.round(value) + '%';
        },
        color: 'inherit'
      },
      data: [{
        value: 50,
        name: '数据'
      }]
    }]
  }
  
  const mergedOptions = { ...defaultOptions, ...options }
  chart.setOption(mergedOptions)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
  
  return chart
}

/**
 * 销毁图表
 * @param {echarts.ECharts} chart - 图表实例
 */
export function destroyChart(chart) {
  if (chart) {
    chart.dispose()
  }
}

/**
 * 销毁地图
 * @param {L.Map} map - 地图实例
 */
export function destroyMap(map) {
  if (map) {
    map.remove()
  }
}
