// ========== UAV 兼容网关 ==========
// 让重构前的前端 100% 不改，直接调用重构后的 API
// 核心功能：路径映射 + 认证转换 + 响应解包

const express = require('express')
const { createProxyMiddleware } = require('http-proxy-middleware')
const jwt = require('jsonwebtoken')
const crypto = require('crypto')
const cors = require('cors')
const morgan = require('morgan')
const config = require('./config')

const app = express()

// ===== 中间件 =====
app.use(cors({ origin: true, credentials: true }))
app.use(express.json({ limit: '10mb' }))
app.use(express.urlencoded({ extended: true, limit: '10mb' }))
app.use(morgan(config.logLevel === 'debug' ? 'dev' : 'combined'))

// ===== 简单内存限流 =====
const rateLimitMap = new Map()
function rateLimit(req, res, next) {
  const clientId = req.ip || req.connection.remoteAddress
  const now = Date.now()
  const windowStart = now - config.rateLimit.windowMs

  if (!rateLimitMap.has(clientId)) {
    rateLimitMap.set(clientId, [])
  }
  const requests = rateLimitMap.get(clientId).filter(t => t > windowStart)
  requests.push(now)
  rateLimitMap.set(clientId, requests)

  if (requests.length > config.rateLimit.maxRequests) {
    return res.status(429).json({
      error: 'Too Many Requests',
      message: '请求过于频繁，请稍后再试'
    })
  }
  next()
}
app.use(rateLimit)

// ===== JWT 解析（提取用户信息）=====
function parseJwt(token) {
  try {
    if (config.jwt.verifySignature) {
      return jwt.verify(token, config.jwt.secret)
    }
    // 不验证签名，只解析 payload
    const payload = token.split('.')[1]
    return JSON.parse(Buffer.from(payload, 'base64').toString())
  } catch (e) {
    return null
  }
}

// ===== HMAC-SHA256 签名生成 =====
function generateHmac(method, path, timestamp, apiKey, secret, body) {
  const bodyStr = body ? JSON.stringify(body) : ''
  const data = `${method.toUpperCase()}\n${path}\n${timestamp}\n${apiKey}\n${bodyStr}`
  return crypto.createHmac('sha256', secret).update(data).digest('base64')
}

// ===== 路径映射查找 =====
function findMapping(path, method) {
  for (const mapping of config.pathMappings) {
    const from = mapping.from
    let matched = false
    let params = []

    if (typeof from === 'string') {
      matched = path === from
    } else if (from instanceof RegExp) {
      const match = path.match(from)
      if (match) {
        matched = true
        params = match.slice(1)
      }
    }

    if (matched) {
      if (mapping.method !== '*' && mapping.method !== method.toUpperCase()) {
        continue
      }
      // 替换路径中的捕获组 $1, $2...
      let targetPath = mapping.to
      params.forEach((p, i) => {
        targetPath = targetPath.replace(new RegExp(`\\$${i + 1}`, 'g'), p)
      })
      return targetPath
    }
  }
  return null
}

// ===== 是否是公开路径 =====
function isPublicPath(path) {
  return config.publicPaths.some(p => path === p || path.startsWith(p + '/'))
}

// ===== 认证转换中间件 =====
app.use((req, res, next) => {
  const originalPath = req.path
  const method = req.method

  // 1. 公开路径跳过认证转换
  if (isPublicPath(originalPath)) {
    return next()
  }

  // 2. 提取旧前端的 JWT Token
  const authHeader = req.headers['authorization'] || ''
  const jwtToken = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null

  let tenantId = 'default'
  let userId = null

  if (jwtToken) {
    const payload = parseJwt(jwtToken)
    if (payload) {
      // 从 JWT payload 提取租户 ID 和用户 ID
      tenantId = payload.tenantId || payload.sub || 'default'
      userId = payload.sub || payload.userId || null
    }
  }

  // 3. 生成新系统的 API Key 认证头
  const timestamp = Date.now().toString()
  const apiKey = config.apiKey.key
  const apiSecret = config.apiKey.secret
  const body = ['GET', 'HEAD', 'DELETE'].includes(method.toUpperCase()) ? null : req.body
  const signature = generateHmac(method, req.path, timestamp, apiKey, apiSecret, body)

  // 4. 设置新系统需要的 Header
  req.headers['x-api-key'] = apiKey
  req.headers['x-signature'] = signature
  req.headers['x-timestamp'] = timestamp
  req.headers['x-tenant-id'] = tenantId
  if (userId) {
    req.headers['x-user-id'] = userId
  }

  // 5. 保留原始 Authorization（新系统网关也支持 JWT）
  // 这样新系统可以同时识别两种认证方式

  next()
})

// ===== 路径重写中间件 =====
app.use((req, res, next) => {
  const originalPath = req.path
  const method = req.method

  // 查找路径映射
  const targetPath = findMapping(originalPath, method)

  if (targetPath) {
    console.log(`[Gateway] ${method} ${originalPath} → ${targetPath}`)
    req.url = targetPath + (req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '')
    req.path = targetPath
  } else {
    console.log(`[Gateway] ${method} ${originalPath} → 无映射，透传`)
  }

  next()
})

// ===== 代理中间件 =====
const proxyMiddleware = createProxyMiddleware({
  target: config.targetGateway,
  changeOrigin: true,
  selfHandleResponse: true, // 我们自己处理响应（用于解包）
  on: {
    proxyReq: (proxyReq, req, res) => {
      // 确保 body 被正确转发
      if (req.body && Object.keys(req.body).length > 0) {
        const bodyData = JSON.stringify(req.body)
        proxyReq.setHeader('Content-Type', 'application/json')
        proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData))
        proxyReq.write(bodyData)
      }
    },
    proxyRes: (proxyRes, req, res) => {
      let body = ''
      proxyRes.on('data', chunk => { body += chunk })
      proxyRes.on('end', () => {
        try {
          // 尝试解析响应为 JSON
          const contentType = proxyRes.headers['content-type'] || ''
          if (!contentType.includes('application/json')) {
            // 非 JSON 响应直接透传
            res.status(proxyRes.statusCode)
            Object.keys(proxyRes.headers).forEach(key => {
              if (key !== 'content-encoding' && key !== 'transfer-encoding') {
                res.setHeader(key, proxyRes.headers[key])
              }
            })
            return res.send(body)
          }

          const data = JSON.parse(body)

          // ===== 响应解包：Result<T> → data =====
          // 新系统返回格式: { code: 200, message: "success", data: {...}, requestId: "...", timestamp: ... }
          // 旧前端期望格式: {...}（直接拿到 data 里的内容）
          if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
            // 这是新系统的 Result 包装
            if (data.code >= 200 && data.code < 300) {
              // 成功：解包 data
              const unpacked = data.data
              res.status(proxyRes.statusCode)
              res.setHeader('Content-Type', 'application/json')
              return res.send(JSON.stringify(unpacked))
            } else {
              // 业务错误：转换为旧前端能理解的格式
              res.status(data.code >= 400 && data.code < 500 ? 400 : 500)
              res.setHeader('Content-Type', 'application/json')
              return res.send(JSON.stringify({
                error: data.message || '请求失败',
                code: data.code,
                requestId: data.requestId
              }))
            }
          }

          // 不是 Result 包装，直接透传
          res.status(proxyRes.statusCode)
          res.setHeader('Content-Type', 'application/json')
          res.send(body)
        } catch (e) {
          // 解析失败，直接透传
          res.status(proxyRes.statusCode)
          res.setHeader('Content-Type', proxyRes.headers['content-type'] || 'application/json')
          res.send(body)
        }
      })
    },
    error: (err, req, res) => {
      console.error('[Gateway] 代理错误:', err.message)
      res.status(502).json({
        error: 'Bad Gateway',
        message: '后端服务暂时不可用，请稍后再试'
      })
    }
  }
})

app.use(proxyMiddleware)

// ===== 健康检查 =====
app.get('/health', (req, res) => {
  res.json({ status: 'UP', service: 'legacy-gateway', timestamp: new Date().toISOString() })
})

// ===== 启动 =====
app.listen(config.port, () => {
  console.log(`
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║     UAV 兼容网关已启动                                        ║
║                                                              ║
║     监听端口: ${config.port}                                    ║
║     目标网关: ${config.targetGateway}                           ║
║                                                              ║
║     功能:                                                    ║
║     • 路径映射: /v1/* → /api/v1/*                            ║
║     • 认证转换: JWT Bearer → API Key + HMAC                  ║
║     • 响应解包: Result<T> → data                             ║
║                                                              ║
║     前端无需任何修改即可调用！                                  ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
  `)
})
