# UAV Weather Microservices - Project Structure Guide

## 📂 目录结构概览

```
weather/
│
├── 📦 后端服务 (Java/Python) ── services/
│   ├── api-gateway/              # API网关 (端口 8088)
│   ├── uav-platform/            # 主平台服务 (端口 8080)
│   ├── wrf-processor/           # WRF气象处理 (端口 8081)
│   ├── meteor-forecast/          # 气象预测服务 (端口 8082)
│   ├── path-planning/           # 路径规划服务 (端口 8083)
│   ├── data-assimilation/       # 数据同化服务 (端口 8084)
│   ├── fengwu-service/          # 风乌气象模型服务 (端口 8085)
│   ├── weather-collector/       # 气象数据采集 (端口 8086)
│   ├── edge-cloud-coordinator/  # 边云协同 (端口 8000/8765)
│   ├── model-engine/            # 模型引擎 (端口 8087)
│   ├── tianzi-service/          # 天资气象分析 (端口 8090)
│   └── fenglei-service/         # 风雷区域模式 (端口 8091)
│
├── 🐍 算法引擎 ── algorithm-engine/
│   └── algorithm-core/          # 核心算法库 (来自 uav-path-planning-system)
│
├── 🐍 算法平台 (独立子项目)
│   └── data-assimilation-platform/  # 贝叶斯同化平台
│
├── 🌐 前端 (Vue3) ── frontend/
│
├── 🖥️ 边缘计算 (C++/Python) ── edge-sdk/
│   └── uav-edge-sdk/           # 端侧SDK
│
├── 📱 移动端 (Flutter) ── mobile/
│   └── uav-mobile-app/         # Flutter移动应用
│
├── 🔧 公共库 ── common/         # 公共工具模块 (JWT/熔断器/异常处理)
│
├── 📋 部署配置
│   ├── k8s/                     # K8s部署 + 监控 + 服务网格
│   ├── docker-compose.yml       # Docker编排 (主力)
│   ├── docker-compose.dev.yml   # 开发环境覆盖
│   └── docker/                  # Docker 基础镜像 + 备用 compose
│
├── 📖 文档 ── docs/
│   ├── archive/                 # 历史报告归档
│   ├── api/                     # API 文档
│   ├── guides/                  # 使用指南
│   ├── deployment/              # 部署文档
│   └── PROJECT_DOCUMENT_INDEX.md
│
├── 🏗️ 遗留服务 ── legacy/       # 已废弃服务 (不在 docker-compose 中)
│
├── ⚙️ 代码质量 ── config/       # checkstyle, sonar, qodana, owasp
│
└── 🛠️ 工具
    ├── scripts/                # 构建脚本
    ├── tests/                  # 集成测试
    └── tools/                  # 工具集合
```

---

## 📦 后端服务详解

### common (公共工具模块) ⭐

**路径**: `common/`
**说明**: 所有Java服务的公共依赖，提供通用功能

**核心功能**:
```
common/
├── src/main/java/com/uav/common/
│   ├── audit/                 # 安全审计
│   ├── config/                # 配置类
│   ├── dto/                   # 数据传输对象
│   ├── exception/             # 异常处理
│   ├── resilience/            # 弹性机制 ⭐
│   ├── security/              # 安全认证
│   └── utils/                # 工具类
└── src/main/resources/
    ├── application.yml
    └── resilience4j-circuitbreaker.yml
```

### api-gateway (API网关)

**路径**: `services/api-gateway/`
**端口**: 8088
**技术**: Spring Cloud Gateway

**路由配置**:
```yaml
routes:
  - /api/platform/**         → uav-platform:8080
  - /api/wrf/**             → wrf-processor:8081
  - /api/forecast/**        → meteor-forecast:8082
  - /api/planning/**        → path-planning:8083
  - /api/assimilation/**    → data-assimilation:8084
  - /api/fengwu/**          → fengwu-service:8085
  - /api/weather/**         → weather-collector:8086
```

### 后端服务端口映射

| 服务 | 端口 | 路径 |
|------|------|------|
| api-gateway | 8088 | services/api-gateway/ |
| uav-platform | 8080 | services/uav-platform/ |
| wrf-processor | 8081 | services/wrf-processor/ |
| meteor-forecast | 8082 | services/meteor-forecast/ |
| path-planning | 8083 | services/path-planning/ |
| data-assimilation | 8084 | services/data-assimilation/ |
| fengwu-service | 8085 | services/fengwu-service/ |
| weather-collector | 8086 | services/weather-collector/ |
| model-engine | 8087 | services/model-engine/ |
| tianzi-service | 8090 | services/tianzi-service/ |
| fenglei-service | 8091 | services/fenglei-service/ |
| edge-cloud-coordinator | 8000 | services/edge-cloud-coordinator/ |

> 废弃服务 (buoy-weather, ground-station-weather, satellite-weather, radiosonde-weather, detection-drone) 已移至 `legacy/`。

---

## 🐍 Python 算法模块详解

### data-assimilation-platform ⭐

**路径**: `data-assimilation-platform/`
**说明**: 贝叶斯同化核心算法库 (独立子项目)

### edge-cloud-coordinator

**路径**: `services/edge-cloud-coordinator/`
**说明**: 边云协同框架 (WebSocket/Kafka/联邦学习/边缘AI)

### model-engine ⭐

**路径**: `services/model-engine/`
**说明**: 模型引擎 (CNN订正/XGBoost/UNet降尺度/GPR风险/EnKF/多无人机/MPC)

---

## 🌐 前端

**路径**: `frontend/`

**技术栈**: Vue 3.3+ / Vite 4.0+ / TypeScript 5.0+ / Pinia / Vue Router 4.0

---

## 📱 移动端

**路径**: `mobile/uav-mobile-app/`
**技术栈**: Flutter 3.x / Dart / iOS + Android

---

## 🖥️ 边缘SDK

**路径**: `edge-sdk/uav-edge-sdk/`
**语言**: C++ (核心) + Python (快速原型) + pybind11

---

## 📋 部署配置

### Docker Compose

**路径**: `docker-compose.yml`

### Kubernetes

**路径**: `k8s/kubernetes/`

### 监控栈

**路径**: `k8s/monitoring/`

**访问地址**:
| 服务 | URL | 端口 |
|------|-----|------|
| Grafana | http://localhost:3000 | 3000 |
| Prometheus | http://localhost:9090 | 9090 |
| Kibana | http://localhost:5601 | 5601 |

---

## 🎯 快速导航

### 需要添加新服务？

1. 在 `services/` 下创建服务文件夹
2. 添加 `pom.xml` (Java) 或 `requirements.txt` (Python)
3. 引入 `common` 依赖 (Java服务)
4. 添加到 `docker-compose.yml`
5. 配置路由到 `api-gateway`

### 构建命令

```bash
# 构建所有Java服务
mvn clean package -DskipTests

# 构建前端
cd frontend
npm install && npm run build

# 启动所有服务
docker compose up -d
```

---

> **最后更新**: 2026-06-19
> **版本**: 4.0 (目录重组)
> **维护者**: DITHIOTHREITOL
