/**
 * 天气API服务
 * 支持 OpenWeatherMap 和 和风天气 (QWeather)
 */

// 天气API配置存储key
const WEATHER_CONFIG_KEY = 'uav_weather_api_config'

// 默认配置
const defaultConfig = {
  provider: 'demo', // 'demo' | 'openweathermap' | 'qweather'
  openweathermap: {
    apiKey: '',
    baseUrl: 'https://api.openweathermap.org/data/2.5',
    units: 'metric', // metric | imperial | standard
    lang: 'zh_cn'
  },
  qweather: {
    apiKey: '',
    baseUrl: 'https://devapi.qweather.com/v7', // 免费订阅
    // 付费订阅: https://api.qweather.com/v7
    lang: 'zh'
  }
}

/**
 * 获取天气API配置
 */
export function getWeatherConfig() {
  try {
    const raw = localStorage.getItem(WEATHER_CONFIG_KEY)
    if (raw) {
      const saved = JSON.parse(raw)
      return { ...defaultConfig, ...saved }
    }
  } catch (e) {
    console.warn('[WeatherApi] Failed to load config:', e)
  }
  return { ...defaultConfig }
}

/**
 * 保存天气API配置
 */
export function saveWeatherConfig(config) {
  try {
    localStorage.setItem(WEATHER_CONFIG_KEY, JSON.stringify(config))
    return true
  } catch (e) {
    console.warn('[WeatherApi] Failed to save config:', e)
    return false
  }
}

/**
 * 天气图标映射
 */
const weatherIconMap = {
  // OpenWeatherMap icon mapping
  openweathermap: {
    '01d': '☀️', '01n': '🌙',
    '02d': '⛅', '02n': '☁️',
    '03d': '☁️', '03n': '☁️',
    '04d': '☁️', '04n': '☁️',
    '09d': '🌧️', '09n': '🌧️',
    '10d': '🌦️', '10n': '🌧️',
    '11d': '⛈️', '11n': '⛈️',
    '13d': '❄️', '13n': '❄️',
    '50d': '🌫️', '50n': '🌫️'
  },
  // 和风天气 icon mapping (使用天气代码)
  qweather: {
    '100': '☀️', '101': '☁️', '102': '⛅', '103': '⛅', '104': '☁️',
    '150': '☀️', '151': '☁️', '152': '⛅', '153': '⛅',
    '300': '阵雨', '301': '⛈️', '302': '⛈️', '303': '⛈️', '304': '⛈️',
    '305': '🌧️', '306': '🌧️', '307': '🌧️', '308': '🌧️', '309': '🌧️',
    '310': '🌧️', '311': '🌧️', '312': '🌧️', '313': '🌧️', '314': '🌧️',
    '315': '🌧️', '316': '🌧️', '317': '🌧️', '318': '🌧️',
    '399': '🌧️',
    '400': '❄️', '401': '❄️', '402': '❄️', '403': '❄️', '404': '❄️',
    '405': '❄️', '406': '❄️', '407': '❄️', '408': '❄️', '409': '❄️',
    '410': '❄️', '456': '❄️', '457': '❄️', '499': '❄️',
    '500': '🌫️', '501': '🌫️', '502': '🌫️', '503': '🌫️', '504': '🌫️',
    '507': '💨', '508': '💨', '509': '💨', '510': '💨',
    '511': '💨', '512': '💨', '513': '💨', '514': '💨', '515': '💨',
    '900': '🌡️', '901': '🌡️', '902': '🌡️', '999': '🌡️'
  }
}

/**
 * 天气状态描述映射
 */
const weatherDescMap = {
  openweathermap: {
    'clear sky': '晴朗',
    'few clouds': '少云',
    'scattered clouds': '多云',
    'broken clouds': '阴天',
    'overcast clouds': '阴天',
    'shower rain': '阵雨',
    'rain': '雨',
    'light rain': '小雨',
    'moderate rain': '中雨',
    'heavy intensity rain': '大雨',
    'thunderstorm': '雷暴',
    'snow': '雪',
    'mist': '薄雾',
    'fog': '雾',
    'haze': '霾'
  },
  qweather: {
    '晴': '晴朗',
    '多云': '多云',
    '阴': '阴天',
    '阵雨': '阵雨',
    '雷阵雨': '雷阵雨',
    '雷阵雨伴有冰雹': '雷暴冰雹',
    '雨': '雨',
    '小雨': '小雨',
    '中雨': '中雨',
    '大雨': '大雨',
    '暴雨': '暴雨',
    '大暴雨': '大暴雨',
    '特大暴雨': '特大暴雨',
    '强阵雨': '强阵雨',
    '雪': '雪',
    '小雪': '小雪',
    '中雪': '中雪',
    '大雪': '大雪',
    '暴雪': '暴雪',
    '雨夹雪': '雨夹雪',
    '雾': '雾',
    '霾': '霾',
    '浮尘': '浮尘',
    '扬沙': '扬沙',
    '沙尘暴': '沙尘暴'
  }
}

/**
 * 调用 OpenWeatherMap API 获取天气
 */
async function fetchOpenWeatherMap(lat, lng, config) {
  const { apiKey, baseUrl, units, lang } = config.openweathermap
  
  if (!apiKey) {
    throw new Error('OpenWeatherMap API Key 未配置')
  }

  const url = `${baseUrl}/weather?lat=${lat}&lon=${lng}&appid=${apiKey}&units=${units}&lang=${lang}`
  
  const response = await fetch(url)
  
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `HTTP ${response.status}`)
  }
  
  const data = await response.json()
  
  // 解析响应数据
  const iconCode = data.weather?.[0]?.icon || '01d'
  const description = data.weather?.[0]?.description || '未知'
  const temp = Math.round(data.main?.temp || 0)
  const feelsLike = Math.round(data.main?.feels_like || temp)
  const humidity = data.main?.humidity || 0
  const windSpeed = data.wind?.speed || 0
  const windDeg = data.wind?.deg || 0
  
  // 获取图标和中文描述
  const icon = weatherIconMap.openweathermap[iconCode] || '🌡️'
  const desc = weatherDescMap.openweathermap[description.toLowerCase()] || description
  
  return {
    success: true,
    provider: 'openweathermap',
    data: {
      icon,
      description: desc,
      temp: `${temp}°C`,
      feelsLike: `${feelsLike}°C`,
      humidity: `${humidity}%`,
      windSpeed: `${windSpeed} m/s`,
      windDeg,
      raw: data
    }
  }
}

/**
 * 调用和风天气 API 获取天气
 */
async function fetchQWeather(lat, lng, config) {
  const { apiKey, baseUrl, lang } = config.qweather
  
  if (!apiKey) {
    throw new Error('和风天气 API Key 未配置')
  }

  // 和风天气需要先获取城市ID (Location ID)
  // 使用经纬度直接查询
  const location = `${lng},${lat}`
  const url = `${baseUrl}/weather/now?location=${location}&key=${apiKey}&lang=${lang}`
  
  const response = await fetch(url)
  
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `HTTP ${response.status}`)
  }
  
  const result = await response.json()
  
  // 检查响应状态
  if (result.code !== '200') {
    const errorMessages = {
      '400': '请求错误',
      '401': 'API Key 无效',
      '402': '超过访问次数',
      '403': '无访问权限',
      '404': '查询的数据不存在',
      '500': '服务器错误'
    }
    throw new Error(errorMessages[result.code] || `错误代码: ${result.code}`)
  }
  
  const data = result.now
  
  // 解析响应数据
  const iconCode = data.icon || '100'
  const description = data.text || '未知'
  const temp = data.temp || '0'
  const feelsLike = data.feelsLike || temp
  const humidity = data.humidity || '0'
  const windSpeed = data.windSpeed || '0'
  const windDir = data.windDir || '未知'
  const windScale = data.windScale || '0'
  
  // 获取图标
  const icon = weatherIconMap.qweather[iconCode] || '🌡️'
  const desc = weatherDescMap.qweather[description] || description
  
  return {
    success: true,
    provider: 'qweather',
    data: {
      icon,
      description: desc,
      temp: `${temp}°C`,
      feelsLike: `${feelsLike}°C`,
      humidity: `${humidity}%`,
      windSpeed: `${windSpeed} km/h`,
      windDir,
      windScale: `${windScale}级`,
      raw: data
    }
  }
}

/**
 * 生成模拟天气数据
 */
function generateDemoWeather(lat, lng) {
  // 根据纬度判断区域
  let region = 'unknown'
  if (lat >= 35 && lat <= 55 && lng >= 100 && lng <= 135) {
    region = 'north'
  } else if (lat >= 25 && lat < 35 && lng >= 110 && lng <= 125) {
    region = 'east'
  } else if (lat >= 18 && lat < 30 && lng >= 105 && lng <= 120) {
    region = 'south'
  }
  
  const weatherData = {
    north: [
      { icon: '🌤️', description: '晴转多云', temp: '20°C' },
      { icon: '☁️', description: '阴天', temp: '18°C' },
      { icon: '💨', description: '大风', temp: '16°C' }
    ],
    east: [
      { icon: '☀️', description: '晴朗', temp: '26°C' },
      { icon: '⛅', description: '多云', temp: '24°C' },
      { icon: '🌧️', description: '小雨', temp: '22°C' }
    ],
    south: [
      { icon: '⛈️', description: '雷阵雨', temp: '28°C' },
      { icon: '⛅', description: '多云', temp: '30°C' },
      { icon: '☀️', description: '晴朗', temp: '32°C' }
    ],
    unknown: [
      { icon: '☀️', description: '晴朗', temp: '25°C' },
      { icon: '⛅', description: '多云', temp: '23°C' }
    ]
  }
  
  const weathers = weatherData[region]
  const randomIndex = Math.floor(Math.random() * weathers.length)
  const weather = weathers[randomIndex]
  
  return {
    success: true,
    provider: 'demo',
    data: {
      icon: weather.icon,
      description: weather.description,
      temp: weather.temp,
      feelsLike: weather.temp,
      humidity: '65%',
      windSpeed: '3 m/s',
      windDir: '东南风',
      windScale: '2级',
      region
    }
  }
}

/**
 * 获取当前天气
 * @param {number} lat 纬度
 * @param {number} lng 经度
 * @returns {Promise<Object>} 天气数据
 */
export async function getCurrentWeather(lat, lng) {
  const config = getWeatherConfig()
  
  try {
    switch (config.provider) {
      case 'openweathermap':
        return await fetchOpenWeatherMap(lat, lng, config)
      
      case 'qweather':
        return await fetchQWeather(lat, lng, config)
      
      case 'demo':
      default:
        return generateDemoWeather(lat, lng)
    }
  } catch (error) {
    console.warn('[WeatherApi] Failed to fetch weather:', error.message)
    
    // 如果真实API失败，降级到模拟数据
    return {
      success: false,
      provider: config.provider,
      error: error.message,
      fallback: generateDemoWeather(lat, lng)
    }
  }
}

/**
 * 检查API配置是否有效
 */
export function validateWeatherConfig(config) {
  const result = {
    valid: false,
    provider: config.provider,
    message: ''
  }
  
  switch (config.provider) {
    case 'openweathermap':
      if (!config.openweathermap?.apiKey) {
        result.message = '请输入 OpenWeatherMap API Key'
      } else {
        result.valid = true
        result.message = '配置有效'
      }
      break
    
    case 'qweather':
      if (!config.qweather?.apiKey) {
        result.message = '请输入和风天气 API Key'
      } else {
        result.valid = true
        result.message = '配置有效'
      }
      break
    
    case 'demo':
      result.valid = true
      result.message = '使用模拟天气数据'
      break
    
    default:
      result.message = '请选择天气数据源'
  }
  
  return result
}

export default {
  getWeatherConfig,
  saveWeatherConfig,
  getCurrentWeather,
  validateWeatherConfig
}
