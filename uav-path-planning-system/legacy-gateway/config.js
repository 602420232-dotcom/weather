// ========== 兼容网关配置 ==========

module.exports = {
  // 网关监听端口（避免与原后端 8089 冲突）
  port: 8090,

  // 重构后 API 网关地址（新系统统一入口）
  targetGateway: 'http://localhost:8080',

  // JWT 配置（用于解析旧前端的 Bearer Token）
  jwt: {
    // 如果新系统使用不同的 JWT secret，在这里配置
    // 默认不验证签名，只解析 payload 提取 tenantId
    verifySignature: false,
    secret: process.env.JWT_SECRET || 'legacy-jwt-secret'
  },

  // API Key 配置（用于调用新系统）
  apiKey: {
    // 兼容网关作为"平台自身"调用新系统的凭证
    // 实际部署时，这里应该是平台在 platform-api 中注册的 API Key
    key: process.env.API_KEY || 'legacy_gateway_key',
    secret: process.env.API_SECRET || 'legacy_gateway_secret'
  },

  // 路径映射规则：原路径 → 新路径
  // 支持字符串精确匹配或正则匹配
  pathMappings: [
    // 认证相关（保留原路径，透传到新系统认证服务）
    { from: '/v1/auth/login',        to: '/api/v1/platform/auth/login',     method: 'POST' },
    { from: '/v1/auth/refresh',      to: '/api/v1/platform/auth/refresh',   method: 'POST' },
    { from: '/v1/auth/register',     to: '/api/v1/platform/auth/register',  method: 'POST' },
    { from: '/v1/auth/logout',       to: '/api/v1/platform/auth/logout',    method: 'POST' },

    // 气象数据
    { from: /^\/v1\/weather\/forecast/,  to: '/api/v1/weather/point',         method: 'POST' },
    { from: /^\/v1\/weather\/heatmap/,    to: '/api/v1/weather/region',        method: 'GET' },
    { from: /^\/v1\/weather\/wind/,       to: '/api/v1/weather/wind-profile',  method: 'POST' },
    { from: /^\/v1\/weather/,             to: '/api/v1/weather/point',         method: 'POST' },

    // 无人机管理 → 规划服务
    { from: /^\/v1\/drones/,              to: '/api/v1/planning/drones',       method: 'GET' },
    { from: /^\/v1\/drones\/([^/]+)$/,   to: '/api/v1/planning/drones/$1',    method: 'GET' },

    // 任务管理 → 规划服务
    { from: /^\/v1\/tasks$/,              to: '/api/v1/planning/tasks',        method: 'GET' },
    { from: /^\/v1\/tasks$/,              to: '/api/v1/planning/tasks',        method: 'POST' },
    { from: /^\/v1\/tasks\/([^/]+)$/,    to: '/api/v1/planning/tasks/$1',     method: 'GET' },
    { from: /^\/v1\/tasks\/([^/]+)\/status/, to: '/api/v1/planning/tasks/$1/status', method: 'PUT' },
    { from: /^\/v1\/tasks\/([^/]+)\/cancel/, to: '/api/v1/planning/tasks/$1/cancel', method: 'POST' },

    // 路径规划
    { from: /^\/v1\/planning\/path/,      to: '/api/v1/planning/path',         method: 'POST' },
    { from: /^\/v1\/planning\/mission/,    to: '/api/v1/planning/mission',      method: 'POST' },
    { from: /^\/v1\/planning\/optimize/,   to: '/api/v1/planning/path',         method: 'POST' },

    // 数据同化
    { from: /^\/v1\/assimilation\/tasks$/,     to: '/api/v1/assimilation/tasks',        method: 'POST' },
    { from: /^\/v1\/assimilation\/tasks$/,     to: '/api/v1/assimilation/tasks',        method: 'GET' },
    { from: /^\/v1\/assimilation\/tasks\/([^/]+)$/, to: '/api/v1/assimilation/tasks/$1', method: 'GET' },
    { from: /^\/v1\/assimilation\/tasks\/([^/]+)\/result/, to: '/api/v1/assimilation/tasks/$1/result', method: 'GET' },
    { from: /^\/v1\/assimilation\/tasks\/([^/]+)\/cancel/, to: '/api/v1/assimilation/tasks/$1/cancel', method: 'POST' },

    // 风险评估
    { from: /^\/v1\/risk\/assess/,        to: '/api/v1/risk/assess',           method: 'POST' },
    { from: /^\/v1\/risk\/map/,           to: '/api/v1/risk/map',              method: 'GET' },
    { from: /^\/v1\/risk\/history/,       to: '/api/v1/risk/history',          method: 'GET' },

    // 适航评估
    { from: /^\/v1\/airworthiness\/assess/,   to: '/api/v1/airworthiness/assess',  method: 'POST' },
    { from: /^\/v1\/airworthiness\/standards/, to: '/api/v1/airworthiness/standards', method: 'GET' },

    // 主动观测
    { from: /^\/v1\/observation\/tasks$/,      to: '/api/v1/observation/tasks',       method: 'POST' },
    { from: /^\/v1\/observation\/tasks$/,      to: '/api/v1/observation/tasks',       method: 'GET' },
    { from: /^\/v1\/observation\/tasks\/([^/]+)$/, to: '/api/v1/observation/tasks/$1', method: 'GET' },
    { from: /^\/v1\/observation\/decisions/,   to: '/api/v1/observation/decisions',   method: 'POST' },

    // UTM
    { from: /^\/v1\/utm\/airspaces/,      to: '/api/v1/airspaces',             method: 'GET' },
    { from: /^\/v1\/utm\/flight-plans$/,   to: '/api/v1/flight-plans',          method: 'POST' },
    { from: /^\/v1\/utm\/flight-plans$/,   to: '/api/v1/flight-plans',          method: 'GET' },
    { from: /^\/v1\/utm\/flight-plans\/([^/]+)$/, to: '/api/v1/flight-plans/$1', method: 'GET' },
    { from: /^\/v1\/utm\/tracking\/positions/, to: '/api/v1/tracking/positions', method: 'POST' },
    { from: /^\/v1\/utm\/tracking\/uavs\/([^/]+)/, to: '/api/v1/tracking/uavs/$1', method: 'GET' },

    // 系统/监控（透传）
    { from: /^\/v1\/system\/health/,      to: '/api/v1/system/health',         method: 'GET' },
    { from: /^\/v1\/system\/info/,        to: '/api/v1/system/info',           method: 'GET' },
    { from: /^\/v1\/monitor/,             to: '/api/v1/monitor',               method: 'GET' },

    // 默认兜底：/v1/xxx → /api/v1/xxx（保持路径不变，只加前缀）
    { from: /^\/v1\/(.*)$/,               to: '/api/v1/$1',                    method: '*' }
  ],

  // 不需要认证的路径（白名单）
  publicPaths: [
    '/v1/auth/login',
    '/v1/auth/register',
    '/v1/system/health',
    '/v1/system/info'
  ],

  // 限流配置（简单内存限流）
  rateLimit: {
    windowMs: 60 * 1000,  // 1 分钟
    maxRequests: 1000     // 每分钟最多 1000 请求
  },

  // 日志配置
  logLevel: process.env.LOG_LEVEL || 'info'
}
