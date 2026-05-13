# Docker 部署指南

## 目录

- [前置准备](#前置准备)
- [快速启动](#快速启动)
- [项目说明](#项目说明)
- [常见问题](#常见问题)

## 前置准备

### 1. 安装 Docker + Docker Compose

- Docker Desktop（推荐用于Windows/Mac）：<https://www.docker.com/get-started>
- Docker Compose 通常随Docker Desktop一起安装

### 2. 验证安装

```bash
docker --version
docker-compose --version
```

### 3. 克隆项目源码

```bash
git clone https://github.com/602420232-dotcom/weather.git
cd trae
```

### 4. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，至少修改：
# DB_PASSWORD、JWT_SECRET、ENCRYPTION_KEY
```

---

## 快速启动

### 使用 Docker Compose（推荐）

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看运行状态
docker-compose ps
```

### 服务启动顺序

1. 基础设施：MySQL、Redis、Nacos、Kafka
2. 微服务：WRF Processor、Data Assimilation、Meteor Forecast、Path Planning、UAV Platform、Weather Collector、Edge-Cloud Coordinator
3. 网关：API Gateway (8088)
4. 前端（可选）：Vue3应用 (3000)

### 验证部署

```bash
# 检查网关健康状态
curl http://localhost:8088/actuator/health

# 检查平台服务
curl http://localhost:8080/actuator/health
```

---

## 项目说明

### 服务端口映射

| 服务 | 端口 | 模块路径 | 说明 |
|------|:----:|---------|------|
| api-gateway | 8088 | api-gateway/ | API 网关（限流/熔断/路由） |
| uav-platform | 8080 | uav-platform-service/ | 主平台服务 |
| wrf-processor | 8081 | wrf-processor-service/ | WRF气象处理 |
| meteor-forecast | 8082 | meteor-forecast-service/ | 气象预测 |
| path-planning | 8083 | path-planning-service/ | 路径规划 |
| data-assimilation | 8084 | data-assimilation-service/ | 数据同化 |
| weather-collector | 8086 | uav-weather-collector/ | 气象采集 |
| edge-coordinator | 8765 | edge-cloud-coordinator/ | 边云协同 |

### 基础设施连接

| 服务 | 地址 | 说明 |
|------|------|------|
| MySQL | localhost:3306 | 关系数据库 |
| Redis | localhost:6379 | 缓存 |
| Nacos | localhost:8848 | 注册中心/配置中心 |
| Kafka | localhost:9092 | 消息队列 |

---

## 开发模式

### 启动基础设施

```bash
docker-compose up -d mysql redis nacos kafka
```

### 本地运行微服务

```bash
# 安装公共模块
cd common-utils && mvn clean install && cd ..

# 启动特定服务
cd uav-platform-service && mvn spring-boot:run
```

### 常用运维命令

```bash
# 查看服务日志
docker-compose logs -f [service-name]

# 重启服务
docker-compose restart [service-name]

# 查看资源使用
docker stats
```

---

## 常见问题

### Q: 端口被占用怎么办？
修改 `.env` 中对应端口配置，或停止占用程序。

### Q: 如何更新服务？
```bash
docker-compose pull
docker-compose up -d
```

### Q: 如何查看日志？
```bash
docker-compose logs -f [service-name]
```

### Q: 服务启动顺序重要吗？
是的，建议先启动基础设施，再启动微服务，最后启动网关。

---

> **最后更新**: 2026-05-09  
> **版本**: 2.1  
> **维护者**: DITHIOTHREITOL