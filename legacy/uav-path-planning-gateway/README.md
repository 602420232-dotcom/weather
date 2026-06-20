# UAV 兼容网关（Legacy Gateway）

让重构前的前端 **100% 不改代码**，直接调用重构后的 API。

## 核心功能

| 功能 | 说明 |
|------|------|
| **路径映射** | `/v1/weather` → `/api/v1/weather/point` 等 |
| **认证转换** | 旧 JWT Bearer → 新 API Key + HMAC 签名 |
| **响应解包** | `Result<T>` → 直接返回 `data` |
| **限流保护** | 每分钟 1000 请求 |

## 快速启动

```bash
# 1. 安装依赖
cd legacy-gateway
npm install

# 2. 配置环境变量（可选，默认配置即可开发）
cp .env.example .env
# 编辑 .env 设置 TARGET_GATEWAY 和 API_KEY

# 3. 启动网关
npm start

# 网关将监听 localhost:8089
```

## 前端配置（无需修改业务代码）

前端 `vite.config.js` 的代理配置**保持不变**：

```javascript
server: {
  proxy: {
    '/v1': {
      target: 'http://localhost:8089',  // 兼容网关地址
      changeOrigin: true
    }
  }
}
```

前端 `src/api/index.js` 的 `baseURL` 也**保持不变**：

```javascript
const api = axios.create({
  baseURL: '/api',  // 不变
  // ...
})
```

## 路径映射规则

详见 `config.js` 中的 `pathMappings` 数组。

主要映射：

| 原路径 | 新路径 | 服务 |
|--------|--------|------|
| `/v1/weather/forecast` | `/api/v1/weather/point` | weather-api |
| `/v1/drones` | `/api/v1/planning/drones` | planning-api |
| `/v1/tasks` | `/api/v1/planning/tasks` | planning-api |
| `/v1/planning/path` | `/api/v1/planning/path` | planning-api |
| `/v1/assimilation/tasks` | `/api/v1/assimilation/tasks` | assimilation-api |
| `/v1/risk/assess` | `/api/v1/risk/assess` | risk-api |
| `/v1/airworthiness/assess` | `/api/v1/airworthiness/assess` | risk-api |
| `/v1/observation/tasks` | `/api/v1/observation/tasks` | observation-api |
| `/v1/utm/flight-plans` | `/api/v1/flight-plans` | utm-api |

## 认证流程

```
旧前端请求
  Authorization: Bearer <jwt>
  ↓
兼容网关
  1. 解析 JWT → 提取 tenantId, userId
  2. 生成 HMAC 签名
  3. 添加新系统 Header:
     X-API-Key: <key>
     X-Signature: <hmac>
     X-Timestamp: <timestamp>
     X-Tenant-Id: <tenantId>
     X-User-Id: <userId>
  4. 保留原 Authorization（新网关也支持 JWT）
  ↓
新系统网关
  认证通过 → 路由到对应微服务
  ↓
微服务返回 Result<T>
  ↓
兼容网关
  解包 Result → 只返回 data
  ↓
旧前端收到
  直接可用的数据体（与之前完全一致）
```

## 生产环境部署

```bash
# 使用 PM2 守护进程
npm install -g pm2
pm2 start server.js --name uav-legacy-gateway

# 或者使用 Docker
docker build -t uav-legacy-gateway .
docker run -d -p 8089:8089 --env-file .env uav-legacy-gateway
```

## 注意事项

1. **API Key 注册**：实际部署前，需要在 `platform-api` 中注册一个 API Key，将 `key` 和 `secret` 填入 `.env`
2. **JWT Secret**：如果新旧系统 JWT secret 不同，设置 `JWT_VERIFY_SIGNATURE=true` 并配置正确的 `JWT_SECRET`
3. **HTTPS**：生产环境建议通过 Nginx 反向代理启用 HTTPS
4. **路径扩展**：如需添加新的路径映射，编辑 `config.js` 中的 `pathMappings` 数组
