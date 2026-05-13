# 生产环境配置指南

## 重要安全配置

本指南说明了如何为生产环境配置关键的安全凭证。

---

## 1. JWT密钥配置

### 重要：
- JWT密钥用于签名和验证JSON Web Tokens
- 密钥必须足够强（至少32字符）
- 生产环境绝对不能使用默认密钥或空密钥

### 配置方法

#### Spring Boot应用
在application-prod.yml或环境变量中配置：

```yaml
uav:
  jwt:
    enabled: true
    secret: ${JWT_SECRET}
    expiration: 86400000
```

#### 环境变量（推荐）
```bash
export JWT_SECRET=$(openssl rand -base64 32)
```

---

## 2. 数据库密码配置

### 重要：
- 数据库密码保护所有业务数据
- 必须使用强密码
- 绝对不能在代码中硬编码密码

### 配置方法

#### Spring Boot应用
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=true
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

---

## 3. Redis密码配置

### 配置方法

```yaml
spring:
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    ssl: true
```

---

## 4. 环境变量配置脚本

### Linux/macOS (.env文件)
```bash
JWT_SECRET=${JWT_SECRET}
DB_HOST=prod-db.example.com
DB_PORT=3306
DB_NAME=uav_platform
DB_USERNAME=uav_app
DB_PASSWORD=${DB_PASSWORD}
REDIS_HOST=prod-redis.example.com
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}
WEATHER_API_KEY=${WEATHER_API_KEY}
```

---

## 5. Kubernetes配置

### Secret对象
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: uav-secrets
type: Opaque
stringData:
  jwt-secret: ${JWT_SECRET_BASE64}
  db-password: ${DB_PASSWORD_BASE64}
  redis-password: ${REDIS_PASSWORD_BASE64}
```

---

## 6. Docker配置

### docker-compose.prod.yml
```yaml
version: '3.8'
services:
  app:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_SECRET=${JWT_SECRET}
      - DB_HOST=${DB_HOST}
      - DB_PASSWORD=${DB_PASSWORD}
```

---

## 7. 安全最佳实践

### 应该做的：
1. 使用环境变量注入敏感配置
2. 定期轮换密钥（建议30天）
3. 使用密钥管理服务
4. 启用审计日志

### 不应该做的：
1. 不要硬编码密码
2. 不要将密钥提交到Git
3. 不要在日志中打印密钥
4. 不要使用弱密码

---

## 8. 快速参考

### 必需的环境变量
```bash
JWT_SECRET=<至少32字符的强密钥>
DB_HOST=<数据库主机>
DB_PORT=<数据库端口>
DB_NAME=<数据库名称>
DB_USERNAME=<数据库用户名>
DB_PASSWORD=<数据库密码>
REDIS_HOST=<Redis主机>
REDIS_PORT=<Redis端口>
REDIS_PASSWORD=<Redis密码>
```

> **最后更新**: 2026-05-09  
> **版本**: 2.1  
> **维护者**: DITHIOTHREITOL