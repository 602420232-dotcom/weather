# 基于WRF气象驱动的无人机VRP智能路径规划系统

本项目面向城市低空物流、电力巡检、应急救援、农林植保、城市管理等场景，构建一套集高精度低空气象预报、多约束智能路径规划、全栈Web可视化管理于一体的无人机智能调度系统。

## 📋 目录

- [系统架构](#系统架构)
- [项目结构](#项目结构)
- [功能特性](#功能特性)
- [四大气象模型](#四大气象模型)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [服务与端口](#服务与端口)
- [详细文档](#详细文档)
- [开发指南](#开发指南)
- [部署指南](#部署指南)
- [故障排除](#故障排除)

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                     UAV Weather Microservices                       │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │   Frontend   │  │   Platform   │  │  Monitoring  │               │
│  │    Vue3      │  │   Service    │  │  Dashboard   │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │     WRF      │  │    Bayes     │  │   Meteor     │               │
│  │  Processor   │  │ Assimilator  │  │  Forecaster  │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
├─────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │              Model-Engine (8087)                              │  │
│  │  CMA数据 → CNN订正 → U-Net降尺度 → GPR风险场 → 融合               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                    ┌──────────────┐                                 │
│                    │    Path      │                                 │
│                    │  Planner     │                                 │
│                    └──────────────┘                                 │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐            │
│  │   Mobile     │  │ Edge SDK     │  │ Edge Cloud   │            │
│  │   App        │  │ (Offline)    │  │ Coordinator  │            │
│  └──────────────┘  └──────────────┘  └──────────────┘            │
└─────────────────────────────────────────────────────────────────────┘
```

## 📂 项目结构

```
weather/
├── services/                       # 全部微服务 (Java + Python)
│   ├── api-gateway/               # API 网关 (Spring Cloud Gateway，端口 8088)
│   ├── uav-platform/              # 主平台服务 (端口 8080)
│   ├── wrf-processor/             # WRF 气象数据处理 (端口 8081)
│   ├── meteor-forecast/           # 气象预测服务 (端口 8082)
│   ├── path-planning/             # 路径规划服务 (端口 8083)
│   ├── data-assimilation/         # 数据同化服务 (端口 8084)
│   ├── fengwu-service/            # 风乌气象模型推理 (端口 8085)
│   ├── weather-collector/         # 气象数据采集 (端口 8086)
│   ├── model-engine/              # AI 模型训练引擎 (端口 8087)
│   ├── tianzi-service/            # 天资气象分析 (端口 8090)
│   ├── fenglei-service/           # 风雷区域模式 (端口 8091)
│   └── edge-cloud-coordinator/    # 边云协同 (端口 8000/8765)
│
├── algorithm-engine/              # Python 算法引擎
│   └── algorithm-core/            # 核心算法库
│
├── data-assimilation-platform/    # 贝叶斯同化平台 (独立子项目)
│
├── frontend/                      # Vue3 前端 (端口 3000)
│
├── mobile/                        # Flutter 移动应用
│   └── uav-mobile-app/
│
├── edge-sdk/                      # C++/Python 边缘 SDK
│   └── uav-edge-sdk/
│
├── common/                        # Java 公共工具库 (JWT/熔断器/异常)
│
├── legacy/                        # 废弃服务 (不在 docker-compose)
│   ├── buoy-weather-service/
│   ├── ground-station-weather-service/
│   ├── satellite-weather-service/
│   ├── radiosonde-weather-service/
│   └── detection-drone-service/
│
├── k8s/                           # Kubernetes 部署
│   ├── kubernetes/                # K8s 部署清单
│   └── monitoring/                # Prometheus/Grafana 监控
│
├── docker/                        # Docker 基础镜像 + 备用 compose
│
├── docs/                          # 文档中心
│   ├── archive/                   # 历史报告归档
│   ├── api/                       # API 接口文档
│   └── guides/                    # 使用指南
│
├── config/                        # 代码质量配置
│   ├── checkstyle.xml
│   ├── sonar-project.properties
│   └── qodana.yaml
│
├── scripts/                       # 构建与管理脚本
├── tests/                         # 项目测试
├── tools/                         # 工具集
│
├── docker-compose.yml             # Docker 编排 (主力)
├── docker-compose.dev.yml         # 开发环境覆盖
├── .env                           # 环境变量 (请勿提交)
├── .env.example                   # 环境变量示例
├── pom.xml                        # Maven 主配置
├── pyproject.toml                 # Python 项目配置
├── README.md
└── LICENSE
```

## ✨ 功能特性

| 功能模块 | 描述 |
|:-------:|:----:|
| **气象预报** | WRF + AI 双引擎，5分钟级高频更新，支持ConvLSTM+XGBoost |
| **数据同化** | 3D-VAR/4D-VAR/EnKF 多算法融合，支持GPU加速 |
| **路径规划** | VRPTW + DE-RRT* + DWA 三层架构，考虑气象约束 |
| **风险评估** | 贝叶斯不确定性分析，动态风险场计算 |
| **动态重规划** | 5秒内响应气象突变，实时更新路径 |
| **边云协同** | 离线评估、边缘任务队列、联邦学习 |
| **移动终端** | Flutter跨平台应用（iOS/Android），实时监控 |
| **Web 平台** | Vue3可视化管理，地图、图表、实时监控一体化 |
| **监控告警** | Prometheus+Grafana，ELK日志聚合 |
| **熔断器保护** | Resilience4j 防止级联故障，完善的监控API |

## 🌤️ 四大气象模型

### SWC-WRF（WRF气象数据处理服务）
- **端口**: 8081
- **路径**: `services/wrf-processor/`
- **功能**: 解析和处理 WRF 模型输出的 NetCDF4 格式气象数据

### FengWu（风乌气象模型推理服务）
- **端口**: 8085
- **路径**: `services/fengwu-service/`
- **功能**: 基于 ONNX 推理引擎运行深度学习全球天气预报模型

### TianZi（天资高分辨率气象分析服务）
- **端口**: 8090
- **路径**: `services/tianzi-service/`
- **功能**: 基于 TianZi 深度学习模型的高分辨率气象分析

### FengLei（风雷区域模式数据服务）
- **端口**: 8091
- **路径**: `services/fenglei-service/`
- **功能**: 提供高分辨率区域气象预报数据 (3km)

### 四模型融合架构

| 模型 | 分辨率 | 权重 | 角色定位 |
|:----:|:------:|:---:|:-------:|
| **SWC-WRF** | 3km | 0.20 | 区域气象数据基础 |
| **FengWu** | 25km | 0.15 | 全球背景场约束 |
| **TianZi** | 25km | 0.25 | 全球模式参考 |
| **FengLei** | 3km | 0.60 | 区域高分辨率核心 |

## 🛠️ 技术栈

| 类别 | 技术 | 版本/说明 |
|:----:|:----:|:--------:|
| **后端框架** | Spring Boot | 3.5.14 |
| **服务发现** | Nacos | 2.x |
| **API 网关** | Spring Cloud Gateway | 2025.0.0 |
| **数据库** | MySQL | 8.0+ |
| **缓存** | Redis | 6.2+ |
| **安全认证** | JWT + Spring Security | 0.12.6 |
| **熔断器** | Resilience4j | 2.3.0 |
| **前端框架** | Vue 3 + TypeScript | - |
| **移动框架** | Flutter/Dart | 3.2.0+ |
| **AI/ML** | PyTorch, XGBoost, ONNX Runtime | - |
| **气象模型** | WRF, FengWu, TianZi, FengLei | - |
| **容器化** | Docker, Docker Compose | 19.03+ |
| **编排** | Kubernetes, ArgoCD | 1.19+ |
| **监控** | Prometheus, Grafana | - |

## 🚀 快速开始

### 环境要求

- **Docker & Docker Compose**: 容器化部署
- **Java 17+**: 后端服务
- **Python 3.8+**: 算法模块
- **Node.js 16+**: 前端开发
- **MySQL 8.0+**: 数据存储
- **Redis 6.2+**: 缓存服务

### 1. 克隆项目

```bash
git clone https://github.com/602420232-dotcom/weather
cd weather
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，配置数据库密码等
```

### 3. 构建并启动服务

```bash
# 先启动基础设施
cd uav-platform-v2
docker compose up -d nacos mysql redis

# 返回根目录，启动微服务
cd ..
docker compose up -d
```

### 4. 验证部署

```bash
# 检查服务状态
docker compose ps

# 访问以下服务
# - 前端应用：http://localhost:3000
# - API 网关：http://localhost:8088/actuator/health
# - Nacos 控制台：http://localhost:8850/nacos
```

## 📡 服务与端口

| 服务名称 | 端口 | 路径 | 健康检查 |
|:-------:|:---:|:----:|:-------:|
| **API Gateway** | 8088 | services/api-gateway/ | `/actuator/health` |
| **WRF Processor** | 8081 | services/wrf-processor/ | `/actuator/health` |
| **Meteor Forecast** | 8082 | services/meteor-forecast/ | `/actuator/health` |
| **Path Planning** | 8083 | services/path-planning/ | `/actuator/health` |
| **Data Assimilation** | 8084 | services/data-assimilation/ | `/actuator/health` |
| **FengWu Service** | 8085 | services/fengwu-service/ | `/health` |
| **TianZi Service** | 8090 | services/tianzi-service/ | `/health` |
| **FengLei Service** | 8091 | services/fenglei-service/ | `/health` |
| **Model Engine** | 8087 | services/model-engine/ | `/health` |
| **Edge Cloud Coordinator** | 8000 | services/edge-cloud-coordinator/ | `/health` |
| **Weather Collector** | 8086 | services/weather-collector/ | `/actuator/health` |
| **Platform** | 8080 | services/uav-platform/ | `/actuator/health` |
| **Frontend** | 3000 | frontend/ | - |
| **MySQL** | 3306 | 容器服务 | - |
| **Redis** | 6379 | 容器服务 | - |
| **Nacos** | 8850 | 容器服务 | - |

## 📚 详细文档

| 文档 | 说明 | 优先级 |
|:----:|:----:|:------:|
| [部署指南](docs/DEPLOYMENT.md) | 完整的部署与运维手册 | ⭐⭐⭐ |
| [架构设计](docs/architecture.md) | 系统架构设计文档 | ⭐⭐⭐ |
| [项目结构](docs/PROJECT_STRUCTURE.md) | 详细目录结构说明 | ⭐⭐ |
| [快速参考](docs/QUICK_REFERENCE.md) | 常用命令和API速查 | ⭐⭐⭐ |
| [API 文档](docs/api/API_DOCUMENTATION.md) | 完整 API 接口文档 | ⭐⭐⭐ |
| [Docker 指南](docs/DOCKER.md) | Docker 使用说明 | ⭐⭐ |
| [端口配置](docs/PORTS_CONFIGURATION.md) | 各服务端口详细说明 | ⭐⭐ |
| [故障排除](docs/guides/TROUBLESHOOTING.md) | 常见问题解答 | ⭐⭐⭐ |

## 👨‍💻 开发指南

### 后端开发

```bash
# 构建项目
mvn clean install -DskipTests

# 在 services/ 下对应目录启动各个微服务
cd services/api-gateway && mvn spring-boot:run
```

### 前端开发

```bash
cd frontend
npm install
npm run dev        # 启动开发服务器
npm run build      # 构建生产版本
```

### 移动应用开发

```bash
cd mobile/uav-mobile-app
flutter pub get
flutter run -d android   # 或 ios
```

### AI 模型训练

```bash
cd services/model-engine
python scripts/train_all.py
```

## 📦 部署指南

### Docker Compose

```bash
# 完整启动
cd uav-platform-v2 && docker compose up -d nacos mysql redis
cd .. && docker compose up -d

# 停止所有服务
docker compose stop
docker compose -f uav-platform-v2/docker-compose.yml stop
```

### Kubernetes

```bash
cd k8s/kubernetes
kubectl apply -f namespace.yml
kubectl apply -f .
```

## 🔧 故障排除

| 问题 | 可能原因 | 解决方案 |
|:----:|:-------:|:-------:|
| **服务启动失败** | 数据库连接失败 | 检查 .env 文件中的 DB 配置 |
| **端口已被占用** | 端口被其他进程占用 | 修改端口配置或停止占用进程 |
| **Nacos 服务注册失败** | Nacos 未就绪 | 检查 Nacos 状态，等待其完全启动 |
| **前端无法连接后端** | CORS 或网络问题 | 检查 nginx 代理配置 |

### 调试命令

```bash
# 查看服务日志
docker compose logs <service-name> -f

# 进入容器
docker compose exec <service-name> sh
```

## 📚 模块文档索引

| 模块 | 文档路径 |
|:----:|:-------:|
| **api-gateway** | [README](services/api-gateway/README.md) |
| **wrf-processor** | [README](services/wrf-processor/README.md) |
| **data-assimilation** | [README](services/data-assimilation/README.md) |
| **meteor-forecast** | [README](services/meteor-forecast/README.md) |
| **path-planning** | [README](services/path-planning/README.md) |
| **uav-platform** | [README](services/uav-platform/README.md) |
| **weather-collector** | [README](services/weather-collector/README.md) |
| **fengwu-service** | [README](services/fengwu-service/README.md) |
| **tianzi-service** | [README](services/tianzi-service/README.md) |
| **fenglei-service** | [README](services/fenglei-service/README.md) |
| **edge-cloud-coordinator** | [README](services/edge-cloud-coordinator/README.md) |
| **model-engine** | [README](services/model-engine/README.md) |
| **common** | [README](common/README.md) |
| **frontend** | [README](frontend/README.md) |
| **mobile** | [README](mobile/README.md) |
| **edge-sdk** | [README](edge-sdk/uav-edge-sdk/README.md) |
| **data-assimilation-platform** | [README](data-assimilation-platform/README.md) |
| **legacy services** | [README](legacy/buoy-weather-service/README.md) 等 |

---

> **最后更新**: 2026-06-19 (目录重组 v4.0)
> **维护者**: UAV DevOps Team
